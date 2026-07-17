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
package org.apache.geronimo.mail.testserver;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Semaphore;

import javax.net.ssl.SSLSocketFactory;

import jakarta.mail.internet.MimeMessage;

import org.apache.commons.configuration2.BaseHierarchicalConfiguration;
import org.apache.james.UserEntityValidator;
import org.apache.james.core.Domain;
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
import org.apache.james.pop3server.mailbox.DefaultMailboxAdapterFactory;
import org.apache.james.pop3server.mailbox.MailboxAdapterFactory;
import org.apache.james.pop3server.netty.POP3Server;
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

//James based POP3 or IMAP or SMTP server (for unittesting only)
public class MailServer {

    public static final String USER = "serveruser";
    public static final String PASSWORD = "serverpass";

    private POP3Server pop3Server;
    private IMAPServer imapServer;
    private SMTPServer smtpServer;

    private AlterableDNSServer dnsServer;
    private MemoryDomainList domainList;
    private MemoryUsersRepository usersRepository;
    private FileSystemImpl fileSystem;
    private MockProtocolHandlerLoader protocolHandlerChain;
    private InMemoryIntegrationResources memoryIntegrationResources;
    private InMemoryMailboxManager mailboxManager;
    private MemoryMailRepositoryStore mailRepositoryStore;
    private MemoryRecipientRewriteTable rewriteTable;
    private MemoryMailQueueFactory queueFactory;
    private MemoryMailQueueFactory.MemoryCacheableMailQueue queue;
    private Disposable fetcher;

    private final Semaphore sem = new Semaphore(0);

    public void ensureMsgCount(final int count) throws InterruptedException {
        sem.acquire(count);
    }

    public void start(final SmtpTestConfiguration smtpConfig, final Pop3TestConfiguration pop3Config, final ImapTestConfiguration imapConfig)
            throws Exception {
        setUpServiceManager();

        final RecordingMetricFactory metricFactory = new RecordingMetricFactory();

        smtpServer = new SMTPServer(new SmtpMetricsImpl(metricFactory));
        smtpServer.setDnsService(dnsServer);
        smtpServer.setFileSystem(fileSystem);
        smtpServer.setProtocolHandlerLoader(protocolHandlerChain);

        pop3Server = new POP3Server();
        pop3Server.setFileSystem(fileSystem);
        pop3Server.setProtocolHandlerLoader(protocolHandlerChain);

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

        final MailboxSession session = mailboxManager.createSystemSession(Username.of(USER));
        final MailboxPath mailboxPath = MailboxPath.inbox(session);
        if (!Mono.from(mailboxManager.mailboxExists(mailboxPath, session)).block()) {
            mailboxManager.createMailbox(mailboxPath, session);
        }
        final MessageManager mailbox = mailboxManager.getMailbox(mailboxPath, session);

        fetcher = Flux.from(queue.deQueue())
                .publishOn(Schedulers.boundedElastic())
                .subscribe(item -> deliver(item, mailbox, session));

        smtpConfig.init();
        pop3Config.init();
        imapConfig.init();

        smtpServer.configure(smtpConfig);
        pop3Server.configure(pop3Config);
        imapServer.configure(imapConfig);

        smtpServer.init();
        pop3Server.init();
        imapServer.init();

    }

    private void deliver(final MailQueue.MailQueueItem item, final MessageManager mailbox, final MailboxSession session) {
        try {
            final MimeMessage msg = item.getMail().getMessage();
            final ByteArrayOutputStream bout = new ByteArrayOutputStream();
            msg.writeTo(bout);
            mailbox.appendMessage(MessageManager.AppendCommand.builder().recent().build(bout.toByteArray()), session);
            item.done(MailQueue.MailQueueItem.CompletionStatus.SUCCESS);
            sem.release();
        } catch (final Exception e) {
            e.printStackTrace();
            try {
                item.done(MailQueue.MailQueueItem.CompletionStatus.RETRY);
            } catch (final Exception ignored) {
                // nothing left to do
            }
        }
    }

    public void stop() throws Exception {

        if (fetcher != null) {
            fetcher.dispose();
        }

        if (protocolHandlerChain != null) {
            protocolHandlerChain.dispose();
        }

        if (imapServer != null) {
            imapServer.destroy();
        }

        if (pop3Server != null) {
            pop3Server.destroy();
        }

        if (smtpServer != null) {
            smtpServer.destroy();
        }

    }

    protected void setUpServiceManager() throws Exception {
        dnsServer = new AlterableDNSServer();

        domainList = new MemoryDomainList(dnsServer);
        domainList.configure(DomainListConfiguration.DEFAULT);
        if (!domainList.containsDomain(Domain.of("localhost"))) {
            domainList.addDomain(Domain.of("localhost"));
        }

        usersRepository = MemoryUsersRepository.withoutVirtualHosting(domainList);
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
        mailRepositoryStore = new MemoryMailRepositoryStore(urlStore, new SimpleMailRepositoryLoader(), storeConfiguration);
        mailRepositoryStore.init();

        rewriteTable = new MemoryRecipientRewriteTable();
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
                .put(binder -> binder.bind(MailboxAdapterFactory.class).to(DefaultMailboxAdapterFactory.class))
                .put(binder -> binder.bind(MetricFactory.class).toInstance(new RecordingMetricFactory()))
                .put(binder -> binder.bind(UserEntityValidator.class).toInstance(UserEntityValidator.NOOP))
                .put(binder -> binder.bind(Authorizator.class).toInstance((userId, otherUserId) -> Authorizator.AuthorizationState.ALLOWED))
                .build();

    }

    /**
     * @return the queue
     */
    public MailQueue getQueue() {
        return queue;
    }

    public static int acquirePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (final IOException e) {
            throw new RuntimeException("Unable to allocate a free port", e);
        }
    }

    public static File getAbsoluteFilePathFromClassPath(final String fileNameFromClasspath) throws FileNotFoundException {

        File configFile = null;
        final URL configURL = MailServer.class.getClassLoader().getResource(fileNameFromClasspath);
        if (configURL != null) {
            try {
                configFile = new File(configURL.toURI());
            } catch (final URISyntaxException e) {
                configFile = new File(configURL.getPath());
            }

            if (configFile.exists()) {
                return configFile;
            } else {
                throw new FileNotFoundException("Cannot read from " + configFile.getAbsolutePath() + " (original resource was " + fileNameFromClasspath + ", URL: " + configURL + "), because the file does not exist");
            }

        } else {
            throw new FileNotFoundException("Failed to load " + fileNameFromClasspath + ", because resource cannot be found within the classpath");
        }

    }

    public static abstract class AbstractTestConfiguration extends BaseHierarchicalConfiguration {

        private final int listenerPort = acquirePort();

        /**
         * @return the listenerPort
         */
        public int getListenerPort() {
            return listenerPort;
        }

        public AbstractTestConfiguration enableSSL(final boolean enableStartTLS, final boolean enableSSL) throws FileNotFoundException {
            addProperty("tls.[@startTLS]", enableStartTLS);
            addProperty("tls.[@socketTLS]", enableSSL);
            addProperty("tls.keystore", "file://" + getAbsoluteFilePathFromClassPath("dummykeystore.jks").getAbsolutePath());
            addProperty("tls.secret", "123456");
            return this;
        }

        public void init() {
            addProperty("[@enabled]", true);
            addProperty("bind", "127.0.0.1:" + this.listenerPort);
            addProperty("connectiontimeout", "360000");
            addProperty("helloName", "jamesserver");
            addProperty("helloName.[@autodetect]", false);
            addProperty("gracefulShutdown", false);
        }

    }

    public static class Pop3TestConfiguration extends AbstractTestConfiguration {

        @Override
        public void init() {
            super.init();

            addProperty("helloName", "pop3 on port " + getListenerPort());

            addProperty("handlerchain.[@coreHandlersPackage]", RefinedCoreCmdHandlerLoader.class.getName());

        }

    }

    public static class ImapTestConfiguration extends AbstractTestConfiguration {

        @Override
        public void init() {
            super.init();

            addProperty("helloName", "imap on port " + getListenerPort());
            addProperty("plainAuthDisallowed", false);

        }

    }

    public static class SmtpTestConfiguration extends AbstractTestConfiguration {

        @Override
        public void init() {
            super.init();

            addProperty("authorizedAddresses", "127.0.0.0/8");
            addProperty("auth.requireSSL", false);
            addProperty("verifyIdentity", false);
            addProperty("handlerchain.[@coreHandlersPackage]", org.apache.james.smtpserver.CoreCmdHandlerLoader.class.getName());

        }

        public SmtpTestConfiguration setRequireAuth(final boolean requireAuth) {

            addProperty("authRequired", requireAuth);
            return this;
        }

        public SmtpTestConfiguration setHeloEhloEnforcement(final boolean heloEhloEnforcement) {

            addProperty("heloEhloEnforcement", heloEhloEnforcement);
            return this;
        }

    }

    public static class DummySocketFactory extends SSLSocketFactory {

        @Override
        public Socket createSocket(final String host, final int port) throws IOException, UnknownHostException {
            throw new IOException("dummy socket factory");
        }

        @Override
        public Socket createSocket(final InetAddress host, final int port) throws IOException {
            throw new IOException("dummy socket factory");
        }

        @Override
        public Socket createSocket(final String host, final int port, final InetAddress localHost, final int localPort) throws IOException,
                UnknownHostException {
            throw new IOException("dummy socket factory");
        }

        @Override
        public Socket createSocket(final InetAddress address, final int port, final InetAddress localAddress, final int localPort)
                throws IOException {
            throw new IOException("dummy socket factory");
        }

        @Override
        public Socket createSocket(final Socket arg0, final String arg1, final int arg2, final boolean arg3) throws IOException {
            throw new IOException("dummy socket factory");
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return new String[0];
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return new String[0];
        }

    }

    private final class AlterableDNSServer implements DNSService {

        private InetAddress localhostByName = null;

        @Override
        public Collection<String> findMXRecords(final String hostname) {
            final List<String> res = new ArrayList<String>();
            if (hostname == null) {
                return res;
            }
            if ("james.apache.org".equals(hostname)) {
                res.add("nagoya.apache.org");
            }
            return res;
        }

        @Override
        public Collection<InetAddress> getAllByName(final String host) throws UnknownHostException {
            return ImmutableList.of(getByName(host));
        }

        @Override
        public InetAddress getByName(final String host) throws UnknownHostException {
            if (getLocalhostByName() != null) {
                if ("127.0.0.1".equals(host)) {
                    return getLocalhostByName();
                }
            }

            if ("0.0.0.0".equals(host)) {
                return InetAddress.getByName("0.0.0.0");
            }

            if ("james.apache.org".equals(host)) {
                return InetAddress.getByName("james.apache.org");
            }

            if ("abgsfe3rsf.de".equals(host)) {
                throw new UnknownHostException();
            }

            if ("128.0.0.1".equals(host) || "192.168.0.1".equals(host) || "127.0.0.1".equals(host) || "127.0.0.0".equals(host)
                    || "255.0.0.0".equals(host) || "255.255.255.255".equals(host) || "localhost".equals(host)) {
                return InetAddress.getByName(host);
            }

            throw new UnsupportedOperationException("getByName not implemented in mock for host: " + host);
        }

        @Override
        public Collection<String> findTXTRecords(final String hostname) {
            final List<String> res = new ArrayList<String>();
            if (hostname == null) {
                return res;
            }

            if ("2.0.0.127.bl.spamcop.net.".equals(hostname)) {
                res.add("Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2");
            }
            return res;
        }

        public InetAddress getLocalhostByName() {
            return localhostByName;
        }

        public void setLocalhostByName(final InetAddress localhostByName) {
            this.localhostByName = localhostByName;
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
