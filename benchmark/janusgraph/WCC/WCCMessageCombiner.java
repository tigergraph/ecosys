package WCC;
import org.apache.tinkerpop.gremlin.process.computer.MessageCombiner;
import java.util.Optional;

public class WCCMessageCombiner implements MessageCombiner<Long> {
	private static final Optional<WCCMessageCombiner> INSTANCE = Optional.of(new WCCMessageCombiner());
	private  WCCMessageCombiner() {
		
	}
	
	@Override
	public Long combine(final Long messageA, final Long messageB) {
		return messageA < messageB ? messageA : messageB;
	}

	public static  Optional<WCCMessageCombiner> instance() {
		return INSTANCE;
	}
}

