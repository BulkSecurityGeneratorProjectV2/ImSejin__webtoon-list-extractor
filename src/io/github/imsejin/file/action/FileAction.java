package io.github.imsejin.file.action;

import java.io.File;
import java.util.List;

import io.github.imsejin.file.model.Webtoon;
import io.github.imsejin.file.service.FileService;

/**
 * FileAction
 * 
 * @author SEJIN
 */
public class FileAction {

	private final FileService fileService;

	private final String currentPath;

	public FileAction(FileService fileService) {
		this.fileService = fileService;
		this.currentPath = "D:\\Cartoons\\Webtoons"; // this.fileService.getCurrentAbsolutePath();
	}

	public String getCurrentPath() {
		return this.currentPath;
	}

	/**
	 * Returns list of webtoons
	 * 
	 * @return List of webtoons
	 */
	public List<Webtoon> getWebtoonsList() {
		List<File> filesList = fileService.getFilesList(currentPath);
		List<Webtoon> webtoonsList = fileService.convertFilesListToWebtoonsList(filesList);

		return webtoonsList;
	}

}
