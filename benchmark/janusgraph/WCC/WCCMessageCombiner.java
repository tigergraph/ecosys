/*
* Copyright (c)  2015-now, TigerGraph Inc.
* All rights reserved
* It is provided as it is for benchmark reproducible purpose.
* anyone can use it for benchmark purpose with the 
* acknowledgement to TigerGraph.
* Author: Litong Shen litong.shen@tigergraph.com
*/
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

