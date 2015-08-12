package info.ata4.disunity.cli.command;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.IntegerConverter;

import info.ata4.disunity.builder.AssetBuilder;
import info.ata4.disunity.builder.FontBuilder;
import info.ata4.disunity.builder.ShaderBuilder;
import info.ata4.disunity.builder.TextAssetBuilder;
import info.ata4.disunity.builder.Texture2DBuilder;
import info.ata4.disunity.cli.converters.PathConverter;
import info.ata4.io.DataWriter;
import info.ata4.io.buffer.source.ByteBufferSource;
import info.ata4.log.LogUtils;
import info.ata4.unity.asset.AssetFile;
import info.ata4.unity.asset.ObjectInfo;
import info.ata4.unity.rtti.ObjectData;
import info.ata4.unity.rtti.ObjectSerializer;

@Parameters(
	    commandNames = "asset-build",
	    commandDescription = "Builds an asset from a directory."
	)
public class AssetBuildCommand extends SingleFileCommand {

	private static final Logger L = LogUtils.getLogger();
	
	@Parameter(
        names = {"-o", "--output"},
        description = "Asset output file",
        converter = PathConverter.class
    )
    private Path outFile;
	
	@Parameter(
		names = {"-a", "--asset"},
		description = "Original asset file",
		converter = PathConverter.class
    )
	private Path originalAsset;
	
	@Parameter(
		names = {"-s", "--spare"},
		description = "Spare byte buffer for assesing the asset file size.",
		converter = IntegerConverter.class
    )
	private int spare = 1000;
	
	@Parameter(
		names = {"-s2", "--spare2"},
		description = "Spare byte buffer for assesing serialized object size.",
		converter = IntegerConverter.class
    )
	private int spare2 = 1000;
	
	@Override
	public void handleFile(Path file) throws IOException {
		if (originalAsset == null) {
			originalAsset = file.resolveSibling(file.getFileName().toString().substring(1));
		}
		if (outFile == null) {
			outFile = file.resolveSibling(Paths.get("out.assets"));
		}
		
		AssetFile assetFile = new AssetFile();
		assetFile.load(originalAsset);
		ObjectSerializer serializer = new ObjectSerializer();
		serializer.setSoundData(assetFile.getAudioBuffer());
		serializer.setSpare(spare2);
		
		List<AssetBuilder> builders = Arrays.<AssetBuilder>asList(
				new TextAssetBuilder(file, serializer),
				new Texture2DBuilder(file, serializer),
				new ShaderBuilder(file, serializer),
				new FontBuilder(file, serializer));
		
		int deltaSum = 0;
		int amount = 0;
		List<ObjectData> objects = assetFile.objects();
		for (ObjectData object : objects) {
			for (AssetBuilder builder : builders) {
				if (builder.isEligible(object)) {
					deltaSum += builder.build(object);
					amount++;
					break;
				}
			}
		}
		
		L.log(Level.INFO, "Repacked: {0} files.", amount);
		
		int size = (int) (assetFile.header().fileSize() + deltaSum + spare);
		ByteBuffer outputBuffer = ByteBuffer.allocate(size);
		assetFile.save(new DataWriter(new ByteBufferSource(outputBuffer)));
		
		int realSize = (int)getRealSize(assetFile);
		outputBuffer.putInt(4, realSize);
		outputBuffer.limit(realSize);
		
		outputBuffer.rewind();
		try (WritableByteChannel channel = Channels.newChannel(new FileOutputStream(outFile.toFile()))) {
			channel.write(outputBuffer);
		}
		
	}
	
	public static long getRealSize(AssetFile assetFile) {
		List<ObjectInfo> objectsInfo = new ArrayList<ObjectInfo>(assetFile.objectInfoMap().values());
		ObjectInfo lastObjectInfo = objectsInfo.get(objectsInfo.size()-1);
		return assetFile.header().dataOffset() + lastObjectInfo.offset() + lastObjectInfo.length();
	}
}
