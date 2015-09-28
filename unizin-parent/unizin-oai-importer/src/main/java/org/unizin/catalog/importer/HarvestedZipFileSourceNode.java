package org.unizin.catalog.importer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipFile;

import org.nuxeo.ecm.platform.importer.source.FileSourceNode;
import org.nuxeo.ecm.platform.importer.source.SourceNode;


public class HarvestedZipFileSourceNode extends FileSourceNode {
	private static Pattern FILES_TO_AVOID = 
			Pattern.compile("(?:^__MACOSX)|^\\.|(?:\\.DS_Store)");


	private final ZipFile zipFile;


	public HarvestedZipFileSourceNode(final File file) throws IOException {
		super(file);
		this.zipFile = new ZipFile(file);
	}

	public HarvestedZipFileSourceNode(final ZipFile zipFile) {
		super(new File(zipFile.getName()));
		this.zipFile = zipFile;
	}

	@Override
	public boolean isFolderish() {
		return true;
	}

	@Override
	public List<SourceNode> getChildren() throws IOException {
		return zipFile.stream().filter(ze -> !ze.isDirectory())
				.filter(ze -> !FILES_TO_AVOID.matcher(ze.getName()).find())
				.map(ze -> new ZipEntrySourceNode(zipFile, ze))
				.collect(Collectors.toList());
	}
}
