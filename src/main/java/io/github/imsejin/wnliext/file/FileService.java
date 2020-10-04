package io.github.imsejin.wnliext.file;

import io.github.imsejin.common.constant.DateType;
import io.github.imsejin.common.util.CollectionUtils;
import io.github.imsejin.common.util.FileUtils;
import io.github.imsejin.common.util.FilenameUtils;
import io.github.imsejin.wnliext.common.util.ZipUtils;
import io.github.imsejin.wnliext.file.model.Platform;
import io.github.imsejin.wnliext.file.model.Webtoon;

import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.imsejin.wnliext.common.Constants.file.*;

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
public final class FileService {

    private FileService() {
    }

    /**
     * 해당 경로에 있는 파일과 디렉터리의 리스트를 반환한다.<br>
     * Returns a list of files and directories in the path.
     */
    static List<File> getFiles(String pathName) {
        return Arrays.asList(new File(pathName).listFiles());
    }

    /**
     * Converts list of files and directories to list of webtoons.
     */
    static List<Webtoon> convertToWebtoons(List<File> files) {
        if (files == null) files = new ArrayList<>();

        List<Webtoon> webtoons = files.stream()
                .filter(ZipUtils::isZip)
                .map(FileService::convertFileToWebtoon)
                .distinct() // Removes duplicated webtoons.
                .sorted(Comparator.comparing(Webtoon::getPlatform)
                        .thenComparing(Webtoon::getTitle)) // Sorts list of webtoons.
                .peek(System.out::println) // Prints console logs.
                .collect(Collectors.toList());

        // Prints console logs.
        System.out.println("\r\nTotal " + webtoons.size() + " webtoon" + (webtoons.size() > 1 ? "s" : ""));

        return webtoons;
    }

    /**
     * converts file to webtoon.
     */
    private static Webtoon convertFileToWebtoon(File file) {
        String filename = FilenameUtils.baseName(file);
        Map<String, String> webtoonInfo = classifyWebtoonInfo(filename);

        String title = webtoonInfo.get("title");
        String authors = webtoonInfo.get("authors");
        String platform = webtoonInfo.get("platform");
        String completed = webtoonInfo.get("completed");
        String creationTime = FileUtils.getCreationTime(file)
                .format(DateTimeFormatter.ofPattern(DateType.F_DATE_TIME.value()));
        String fileExtension = FilenameUtils.extension(file);
        long size = file.length();

        return Webtoon.builder()
                .title(title)
                .authors(authors)
                .platform(platform)
                .completed(Boolean.parseBoolean(completed))
                .creationTime(creationTime)
                .fileExtension(fileExtension)
                .size(size)
                .build();
    }

    /**
     * Analyzes the file name and classifies it as platform, title, author and completed.
     */
    private static Map<String, String> classifyWebtoonInfo(String filename) {
        StringBuilder sb = new StringBuilder(filename);

        // Platform
        int i = sb.indexOf(DELIMITER_PLATFORM);
        String platform = convertAcronym(sb.substring(0, i));
        sb.delete(0, i + DELIMITER_PLATFORM.length());

        // Title
        int j = sb.lastIndexOf(DELIMITER_TITLE);
        String title = sb.substring(0, j);
        sb.delete(0, j + DELIMITER_TITLE.length());

        // Completed or uncompleted
        boolean completed = filename.endsWith(DELIMITER_COMPLETED);

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
    private static String convertAcronym(String acronym) {
        return Stream.of(Platform.values())
                .filter(platform -> platform.key().equals(acronym))
                .map(Platform::value)
                .findFirst()
                .orElse(acronym);
    }

    static String getLatestFilename(List<File> files) {
        String latestFilename = null;

        // Shallow copy.
        List<File> dummy = new ArrayList<>(files);

        // Removes non-webtoon-list from list.
        dummy.removeIf(file -> {
            String filename = FilenameUtils.baseName(file);
            String fileExtension = FilenameUtils.extension(file);

            return !file.isFile() || !filename.startsWith(EXCEL_FILE_PREFIX) || !fileExtension.equals(XLSX_FILE_EXTENSION);
        });

        // Sorts out the latest file.
        if (CollectionUtils.exists(dummy)) {
            latestFilename = dummy.stream()
                    .map(File::getName)
                    .max(Comparator.naturalOrder())
                    .get();
        }

        return latestFilename;
    }

}
