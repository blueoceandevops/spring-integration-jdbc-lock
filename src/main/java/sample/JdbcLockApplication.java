package sample;

import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.channel.MessageChannels;
import org.springframework.integration.dsl.core.Pollers;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;
import org.springframework.integration.support.leader.LockRegistryLeaderInitiator;
import org.springframework.integration.support.locks.LockRegistry;

@SpringBootApplication
public class JdbcLockApplication {

	public static void main(String[] args) {
		SpringApplication.run(JdbcLockApplication.class, args);
	}

	@Bean
	public DefaultLockRepository lockRepository(DataSource dataSource) {
		return new DefaultLockRepository(dataSource);
	}

	@Bean
	public JdbcLockRegistry lockRegistry(LockRepository lockRepository) {
		return new JdbcLockRegistry(lockRepository);
	}

	@Bean
	public LockRegistryLeaderInitiator leaderInitiator(LockRegistry lockRegistry) {
		return new LockRegistryLeaderInitiator(lockRegistry);
	}

	@Bean
	public MessageSource<?> integerMessageSource() {
		MethodInvokingMessageSource source = new MethodInvokingMessageSource();
		source.setObject(new AtomicInteger());
		source.setMethodName("getAndIncrement");
		return source;
	}

	@Bean
	public DirectChannel inputChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow myFlow() {
		return IntegrationFlows.from(integerMessageSource(), c -> c.poller(Pollers.fixedRate(100)))
				.channel(this.inputChannel())
				.filter((Integer p) -> p > 0)
				.transform(Object::toString)
				.channel(MessageChannels.queue())
				.get();
	}

}
