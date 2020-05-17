package io.github.imsejin.file.service;

import static io.github.imsejin.common.Constants.file.*;
import static io.github.imsejin.common.util.FileUtil.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.imsejin.common.util.ObjectUtil;
import io.github.imsejin.common.util.ZipUtil;
import io.github.imsejin.file.model.Platform;
import io.github.imsejin.file.model.Webtoon;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * 파일 서비스<br>
 * File service
 * 
 * <p>
 * 압축 파일에서 웹툰 정보를 추출하고 리스트 형태로 반환한다.<br>
 * Extracts the information of webtoon from archive file and returns it in the form of a list.
 * </p>
 * 
 * @author SEJIN
 */
public class FileService {

    /**
     * 애플리케이션이 있는 현재 경로를 반환한다.<br>
     * Returns the current path where the application is.
     */
    @SneakyThrows(IOException.class)
    public String getCurrentPathName() {
        // 이 코드는 `System.getProperty("user.dir")`으로 대체할 수 있다.
        // This code can be replaced with `System.getProperty("user.dir")`.
        return Paths.get(".").toRealPath().toString();
    }

    /**
     * 해당 경로에 있는 파일과 디렉터리의 리스트를 반환한다.<br>
     * Returns a list of files and directories in the path.
     */
    public List<File> getFileList(@NonNull String pathName) {
        File file = new File(pathName);
        return Arrays.asList(file.listFiles());
    }

    /**
     * Converts list of files and directories to list of webtoons.
     */
    public List<Webtoon> convertToWebtoonList(List<File> fileList) {
        if (fileList == null) fileList = new ArrayList<>();

        List<Webtoon> webtoonList = fileList.stream()
                .filter(ZipUtil::isZip)
                .map(this::convertFileToWebtoon)
                .sorted(Comparator.comparing(Webtoon::getPlatform)
                        .thenComparing(Webtoon::getTitle)) // Sorts list of webtoons.
                .peek(System.out::println) // Prints console logs.
                .collect(Collectors.toList());

        // Prints console logs.
        System.out.println("\r\nTotal " + webtoonList.size() + " webtoon" + (webtoonList.size() > 1 ? "s" : ""));

        return webtoonList;
    }

    /**
     * converts file to webtoon.
     */
    private Webtoon convertFileToWebtoon(File file) {
        String fileName = getBaseName(file);
        Map<String, String> webtoonInfo = classifyWebtoonInfo(fileName);

        String title = webtoonInfo.get("title");
        String authors = webtoonInfo.get("authors");
        String platform = webtoonInfo.get("platform");
        String completed = webtoonInfo.get("completed");
        String creationTime = getCreationTime(file);
        String fileExtension = getExtension(file);
        long size = file.length();

        return Webtoon.builder()
                .title(title)
                .authors(authors)
                .platform(platform)
                .completed(Boolean.valueOf(completed))
                .creationTime(creationTime)
                .fileExtension(fileExtension)
                .size(size)
                .build();
    }

    /**
     * Analyzes the file name and classifies it as platform, title, author and completed.
     */
    private Map<String, String> classifyWebtoonInfo(String fileName) {
        StringBuffer sb = new StringBuffer(fileName);

        // Platform
        int i = sb.indexOf(DELIMITER_PLATFORM);
        String platform = convertAcronym(sb.substring(0, i));
        sb.delete(0, i + DELIMITER_PLATFORM.length());

        // Title
        int j = sb.lastIndexOf(DELIMITER_TITLE);
        String title = sb.substring(0, j);
        sb.delete(0, j + DELIMITER_TITLE.length());

        // Completed or uncompleted
        boolean completed = fileName.endsWith(DELIMITER_COMPLETED);

        // Authors
        String authors = completed
                ? sb.substring(0, sb.indexOf(DELIMITER_COMPLETED))
                : sb.toString();

        Map<String, String> map = new HashMap<>();
        map.put("title", title);
        map.put("authors", authors);
        map.put("platform", platform);
        map.put("completed", String.valueOf(completed));

        return map;
    }

    /**
     * Converts acronym of platform to full text.
     */
    private String convertAcronym(String acronym) {
        return Stream.of(Platform.values())
                .filter(platform -> platform.name().equals(acronym))
                .map(Platform::getFullText)
                .findFirst()
                .orElse(acronym);
    }

    public String getLatestFileName(List<File> fileList) {
        String latestFileName = null;

        // Shallow copy.
        List<File> dummy = new ArrayList<>(fileList);

        // Removes non-webtoon-list from list.
        dummy.removeIf(file -> {
            String fileName = getBaseName(file);
            String fileExtension = getExtension(file);

            return !file.isFile() || !fileName.startsWith(EXCEL_FILE_PREFIX) || !fileExtension.equals(XLSX_FILE_EXTENSION);
        });

        // Sorts out the latest file.
        if (ObjectUtil.isNotEmpty(dummy)) {
            latestFileName = dummy.stream().map(File::getName).sorted(Comparator.reverseOrder()).findFirst().get();
        }

        return latestFileName;
    }

}
