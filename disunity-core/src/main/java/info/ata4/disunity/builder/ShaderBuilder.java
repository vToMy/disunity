package info.ata4.disunity.builder;

import java.nio.file.Path;

import info.ata4.disunity.extract.ShaderExtractor;
import info.ata4.unity.rtti.ObjectSerializer;

public class ShaderBuilder extends TextAssetBuilder {

	public ShaderBuilder(Path inputDirectory, ObjectSerializer serializer) {
		super(new ShaderExtractor(), inputDirectory, serializer);
	}
	
	protected String getExtension() {
		return "shader";
	}
}
