/**
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.geronimo.mail.tck;

import java.io.ByteArrayOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.mail.internet.MimeMessage;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.james.UserEntityValidator;
import org.apache.james.core.Domain;
import org.apache.james.core.MailAddress;
import org.apache.james.core.Username;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.domainlist.api.DomainList;
import org.apache.james.domainlist.lib.DomainListConfiguration;
import org.apache.james.domainlist.memory.MemoryDomainList;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.imap.encode.main.DefaultImapEncoderFactory;
import org.apache.james.imap.main.DefaultImapDecoderFactory;
import org.apache.james.imap.processor.fetch.FetchProcessor;
import org.apache.james.imap.processor.main.DefaultImapProcessorFactory;
import org.apache.james.imapserver.netty.IMAPServer;
import org.apache.james.imapserver.netty.ImapMetrics;
import org.apache.james.lifecycle.api.LifecycleUtil;
import org.apache.james.mailbox.Authorizator;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.inmemory.InMemoryMailboxManager;
import org.apache.james.mailbox.inmemory.manager.InMemoryIntegrationResources;
import org.apache.james.mailbox.model.MailboxPath;
import org.apache.james.mailbox.store.StoreSubscriptionManager;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.Protocol;
import org.apache.james.mailrepository.memory.MailRepositoryStoreConfiguration;
import org.apache.james.mailrepository.memory.MemoryMailRepository;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryStore;
import org.apache.james.mailrepository.memory.MemoryMailRepositoryUrlStore;
import org.apache.james.mailrepository.memory.SimpleMailRepositoryLoader;
import org.apache.james.metrics.api.MetricFactory;
import org.apache.james.metrics.api.NoopGaugeRegistry;
import org.apache.james.metrics.tests.RecordingMetricFactory;
import org.apache.james.protocols.lib.mock.MockProtocolHandlerLoader;
import org.apache.james.queue.api.MailQueue;
import org.apache.james.queue.api.MailQueueFactory;
import org.apache.james.queue.api.RawMailQueueItemDecoratorFactory;
import org.apache.james.queue.memory.MemoryMailQueueFactory;
import org.apache.james.rrt.api.AliasReverseResolver;
import org.apache.james.rrt.api.CanSendFrom;
import org.apache.james.rrt.api.RecipientRewriteTable;
import org.apache.james.rrt.api.RecipientRewriteTableConfiguration;
import org.apache.james.rrt.lib.AliasReverseResolverImpl;
import org.apache.james.rrt.lib.CanSendFromImpl;
import org.apache.james.rrt.memory.MemoryRecipientRewriteTable;
import org.apache.james.server.core.filesystem.FileSystemImpl;
import org.apache.james.smtpserver.netty.SMTPServer;
import org.apache.james.smtpserver.netty.SmtpMetricsImpl;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.memory.MemoryUsersRepository;
import org.apache.mailet.Mail;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Standalone embedded Apache James server used as the mail server for the
 * Jakarta Mail TCK: SMTP + IMAP, domain james.local, user user01@james.local.
 * Started by the mail-tck module at pre-integration-test and stopped through a
 * loopback control socket at post-integration-test ("--stop").
 */
public final class JamesTckServer {

    public static final String DOMAIN = "james.local";
    public static final String USER = "user01@" + DOMAIN;
    public static final String PASSWORD = "1234";

    private final int smtpPort;
    private final int imapPort;

    private SMTPServer smtpServer;
    private IMAPServer imapServer;
    private MemoryDomainList domainList;
    private MemoryUsersRepository usersRepository;
    private FileSystemImpl fileSystem;
    private MockProtocolHandlerLoader protocolHandlerChain;
    private InMemoryIntegrationResources memoryIntegrationResources;
    private InMemoryMailboxManager mailboxManager;
    private MemoryMailQueueFactory queueFactory;
    private MemoryMailQueueFactory.MemoryCacheableMailQueue queue;
    private Disposable fetcher;

    public JamesTckServer(final int smtpPort, final int imapPort) {
        this.smtpPort = smtpPort;
        this.imapPort = imapPort;
    }

    public static void main(final String[] args) throws Exception {
        final int controlPort = Integer.getInteger("tck.control.port", 11300);

        if (args.length > 0 && "--stop".equals(args[0])) {
            try (Socket socket = new Socket(InetAddress.getLoopbackAddress(), controlPort)) {
                socket.getOutputStream().write('q');
                socket.getOutputStream().flush();
            }
            return;
        }

        final JamesTckServer server = new JamesTckServer(
                Integer.getInteger("tck.smtp.port", 1025),
                Integer.getInteger("tck.imap.port", 1143));
        server.start();
        System.out.println("TCK James server ready (SMTP=" + server.smtpPort + ", IMAP=" + server.imapPort
                + ", user=" + USER + ")");

        // block until a stop request arrives on the control port
        try (ServerSocket control = new ServerSocket(controlPort, 1, InetAddress.getLoopbackAddress())) {
            try (Socket accepted = control.accept()) {
                accepted.getInputStream().read();
            }
        }
        server.stop();
    }

    public void start() throws Exception {
        final AlterableDNSServer dnsServer = new AlterableDNSServer();

        domainList = new MemoryDomainList(dnsServer);
        domainList.configure(DomainListConfiguration.DEFAULT);
        if (!domainList.containsDomain(Domain.of(DOMAIN))) {
            domainList.addDomain(Domain.of(DOMAIN));
        }

        usersRepository = MemoryUsersRepository.withVirtualHosting(domainList);
        usersRepository.addUser(Username.of(USER), PASSWORD);

        memoryIntegrationResources = InMemoryIntegrationResources.builder()
                .authenticator((userid, passwd) -> {
                    try {
                        return usersRepository.test(userid, passwd.toString());
                    } catch (final UsersRepositoryException e) {
                        e.printStackTrace();
                        return Optional.empty();
                    }
                })
                .fakeAuthorizator()
                .inVmEventBus()
                .defaultAnnotationLimits()
                .defaultMessageParser()
                .scanningSearchIndex()
                .noPreDeletionHooks()
                .storeQuotaManager()
                .build();
        mailboxManager = memoryIntegrationResources.getMailboxManager();

        fileSystem = FileSystemImpl.forTestingWithConfigurationFromClasspath();

        final MemoryMailRepositoryUrlStore urlStore = new MemoryMailRepositoryUrlStore();
        final MailRepositoryStoreConfiguration storeConfiguration = MailRepositoryStoreConfiguration.forItems(
                new MailRepositoryStoreConfiguration.Item(
                        ImmutableList.of(new Protocol("memory")),
                        MemoryMailRepository.class.getName(),
                        new BaseHierarchicalConfiguration()));
        final MemoryMailRepositoryStore mailRepositoryStore =
                new MemoryMailRepositoryStore(urlStore, new SimpleMailRepositoryLoader(), storeConfiguration);
        mailRepositoryStore.init();

        final MemoryRecipientRewriteTable rewriteTable = new MemoryRecipientRewriteTable();
        rewriteTable.setConfiguration(RecipientRewriteTableConfiguration.DEFAULT_ENABLED);
        final AliasReverseResolver aliasReverseResolver = new AliasReverseResolverImpl(rewriteTable);
        final CanSendFrom canSendFrom = new CanSendFromImpl(aliasReverseResolver);

        queueFactory = new MemoryMailQueueFactory(new RawMailQueueItemDecoratorFactory(), Clock.systemUTC());
        queue = queueFactory.createQueue(MailQueueFactory.SPOOL);

        protocolHandlerChain = MockProtocolHandlerLoader.builder()
                .put(binder -> binder.bind(DomainList.class).toInstance(domainList))
                .put(binder -> binder.bind(Clock.class).toInstance(Clock.systemUTC()))
                .put(binder -> binder.bind(new TypeLiteral<MailQueueFactory<?>>() {}).toInstance(queueFactory))
                .put(binder -> binder.bind(RecipientRewriteTable.class).toInstance(rewriteTable))
                .put(binder -> binder.bind(CanSendFrom.class).toInstance(canSendFrom))
                .put(binder -> binder.bind(FileSystem.class).toInstance(fileSystem))
                .put(binder -> binder.bind(MailRepositoryStore.class).toInstance(mailRepositoryStore))
                .put(binder -> binder.bind(DNSService.class).toInstance(dnsServer))
                .put(binder -> binder.bind(UsersRepository.class).toInstance(usersRepository))
                .put(binder -> binder.bind(MailboxManager.class).annotatedWith(Names.named("mailboxmanager")).toInstance(mailboxManager))
                .put(binder -> binder.bind(MetricFactory.class).toInstance(new RecordingMetricFactory()))
                .put(binder -> binder.bind(UserEntityValidator.class).toInstance(UserEntityValidator.NOOP))
                .put(binder -> binder.bind(Authorizator.class).toInstance((userId, otherUserId) -> Authorizator.AuthorizationState.ALLOWED))
                .build();

        final RecordingMetricFactory metricFactory = new RecordingMetricFactory();

        smtpServer = new SMTPServer(new SmtpMetricsImpl(metricFactory));
        smtpServer.setDnsService(dnsServer);
        smtpServer.setFileSystem(fileSystem);
        smtpServer.setProtocolHandlerLoader(protocolHandlerChain);

        imapServer = new IMAPServer(
                new DefaultImapDecoderFactory().buildImapDecoder(),
                new DefaultImapEncoderFactory().buildImapEncoder(),
                DefaultImapProcessorFactory.createXListSupportingProcessor(
                        mailboxManager,
                        memoryIntegrationResources.getEventBus(),
                        new StoreSubscriptionManager(mailboxManager.getMapperFactory(),
                                mailboxManager.getMapperFactory(),
                                mailboxManager.getEventBus()),
                        null,
                        memoryIntegrationResources.getQuotaManager(),
                        memoryIntegrationResources.getQuotaRootResolver(),
                        metricFactory,
                        FetchProcessor.LocalCacheConfiguration.DEFAULT),
                new ImapMetrics(metricFactory),
                new NoopGaugeRegistry(),
                ImmutableSet.of());
        imapServer.setFileSystem(fileSystem);

        // make sure the INBOX exists before any SMTP delivery
        final MailboxSession session = mailboxManager.createSystemSession(Username.of(USER));
        final MailboxPath inbox = MailboxPath.inbox(session);
        if (!Mono.from(mailboxManager.mailboxExists(inbox, session)).block()) {
            mailboxManager.createMailbox(inbox, session);
        }

        fetcher = Flux.from(queue.deQueue())
                .publishOn(Schedulers.boundedElastic())
                .subscribe(this::deliver);

        final BaseHierarchicalConfiguration smtpConfig = serverConfig(smtpPort);
        smtpConfig.addProperty("authorizedAddresses", "127.0.0.0/8");
        smtpConfig.addProperty("auth.requireSSL", false);
        smtpConfig.addProperty("verifyIdentity", false);
        smtpConfig.addProperty("handlerchain.[@coreHandlersPackage]", org.apache.james.smtpserver.CoreCmdHandlerLoader.class.getName());

        final BaseHierarchicalConfiguration imapConfig = serverConfig(imapPort);
        imapConfig.addProperty("plainAuthDisallowed", false);

        smtpServer.configure(smtpConfig);
        imapServer.configure(imapConfig);

        smtpServer.init();
        imapServer.init();
    }

    private static BaseHierarchicalConfiguration serverConfig(final int port) {
        final BaseHierarchicalConfiguration config = new BaseHierarchicalConfiguration();
        config.addProperty("[@enabled]", true);
        config.addProperty("bind", "0.0.0.0:" + port);
        config.addProperty("connectiontimeout", "360000");
        config.addProperty("helloName", DOMAIN);
        config.addProperty("helloName.[@autodetect]", false);
        config.addProperty("gracefulShutdown", false);
        return config;
    }

    /**
     * Local delivery: append the dequeued mail to the INBOX of every local
     * recipient. The dequeued mail is a queue-side copy owned by the consumer
     * and must be disposed (see the MailQueue contract / JamesMailSpooler).
     */
    private void deliver(final MailQueue.MailQueueItem item) {
        final Mail mail = item.getMail();
        try {
            final MimeMessage msg = mail.getMessage();
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            msg.writeTo(bout);
            final byte[] content = bout.toByteArray();

            for (final MailAddress recipient : mail.getRecipients()) {
                final Username username = Username.of(recipient.asString());
                final MailboxSession session = mailboxManager.createSystemSession(username);
                final MailboxPath inbox = MailboxPath.inbox(session);
                if (!Mono.from(mailboxManager.mailboxExists(inbox, session)).block()) {
                    mailboxManager.createMailbox(inbox, session);
                }
                final MessageManager mailbox = mailboxManager.getMailbox(inbox, session);
                mailbox.appendMessage(MessageManager.AppendCommand.builder().recent().build(content), session);
            }
            item.done(MailQueue.MailQueueItem.CompletionStatus.SUCCESS);
        } catch (final Exception e) {
            e.printStackTrace();
            try {
                item.done(MailQueue.MailQueueItem.CompletionStatus.REJECT);
            } catch (final Exception ignored) {
                // nothing left to do
            }
        } finally {
            LifecycleUtil.dispose(mail);
        }
    }

    public void stop() throws Exception {
        if (fetcher != null) {
            fetcher.dispose();
        }
        if (queue != null) {
            queue.close();
        }
        if (protocolHandlerChain != null) {
            protocolHandlerChain.dispose();
        }
        if (imapServer != null) {
            imapServer.destroy();
        }
        if (smtpServer != null) {
            smtpServer.destroy();
        }
    }

    /**
     * Minimal DNS mock: everything the TCK talks to is local.
     */
    private static final class AlterableDNSServer implements DNSService {

        @Override
        public Collection<String> findMXRecords(final String hostname) {
            return new ArrayList<>();
        }

        @Override
        public Collection<InetAddress> getAllByName(final String host) throws UnknownHostException {
            return ImmutableList.of(getByName(host));
        }

        @Override
        public InetAddress getByName(final String host) throws UnknownHostException {
            if (DOMAIN.equals(host) || "localhost".equals(host) || "127.0.0.1".equals(host) || "0.0.0.0".equals(host)) {
                return InetAddress.getByName("127.0.0.1");
            }
            return InetAddress.getByName(host);
        }

        @Override
        public Collection<String> findTXTRecords(final String hostname) {
            return new ArrayList<>();
        }

        @Override
        public String getHostName(final InetAddress addr) {
            return addr.getHostName();
        }

        @Override
        public InetAddress getLocalHost() throws UnknownHostException {
            return InetAddress.getLocalHost();
        }
    }

}
