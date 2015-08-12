package info.ata4.disunity.builder;

import java.io.IOException;

import info.ata4.unity.rtti.ObjectData;

public interface AssetBuilder {

	public int build(ObjectData objectData) throws IOException;
	
	public boolean isEligible(ObjectData objectData);
}
