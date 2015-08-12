package info.ata4.disunity.builder;

import java.io.IOException;
import java.nio.file.Path;

import info.ata4.disunity.extract.AssetExtractor;
import info.ata4.unity.rtti.ObjectData;
import info.ata4.unity.rtti.ObjectSerializer;

public abstract class AbstractAssetBuilder<T extends AssetExtractor> implements AssetBuilder {

	protected T assetExtractor;
	private ObjectSerializer serializer;
	protected Path inputDirectory;
	
	protected AbstractAssetBuilder(T assetExtractor, Path inputDirectory, ObjectSerializer serializer) {
		this.assetExtractor = assetExtractor;
		this.inputDirectory = inputDirectory;
		this.serializer = serializer;
	}
	
	@Override
	public int build(ObjectData objectData) throws IOException {
		return serialize(objectData, serializer);
	}
	
	public abstract int serialize(ObjectData objectData, ObjectSerializer serializer) throws IOException;
	
	@Override
	public boolean isEligible(ObjectData objectData) {
		return assetExtractor.isEligible(objectData);
	}
}
