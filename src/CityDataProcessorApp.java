import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public class CityDataProcessorApp {
    public static void main(String[] args) {
        UserInterface ui = new UserInterface(new FileProcessor());
        ui.run();
    }
}

class UserInterface {
    private final FileProcessor fileProcessor;

    public UserInterface(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    public void run() {
        Scanner scanner = new Scanner(System.in);
        String input;

        while (true) {
            System.out.println("Введите путь до файла-справочника или 'exit' для завершения работы:");
            input = scanner.nextLine().trim();

            if ("exit".equalsIgnoreCase(input)) {
                System.out.println("Завершение работы приложения.");
                break;
            }

            Path filePath = Path.of(input);
            if (!Files.exists(filePath)) {
                System.out.println("Файл не найден. Попробуйте снова.");
                continue;
            }

            try {
                Instant start = Instant.now();
                fileProcessor.processFile(filePath);
                Instant end = Instant.now();
                System.out.println("Время обработки файла: " + Duration.between(start, end).toMillis() + " мс\n");
            } catch (UnsupportedFileFormatException e) {
                System.out.println(e.getMessage());
            } catch (Exception e) {
                System.out.println("Произошла ошибка при обработке файла: " + e.getMessage());
            }
        }

        scanner.close();
    }
}

class FileProcessor {
    public void processFile(Path filePath) throws Exception {
        String fileType = getFileExtension(filePath);

        FileHandler handler = switch (fileType.toLowerCase()) {
            case "xml" -> new XmlFileHandler();
            case "csv" -> new CsvFileHandler();
            default -> throw new UnsupportedFileFormatException("Неподдерживаемый формат файла. Поддерживаются только XML и CSV.");
        };

        FileStatistics stats = handler.process(filePath);
        stats.printStatistics();
    }

    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        return (lastDotIndex == -1) ? "" : fileName.substring(lastDotIndex + 1);
    }
}

interface FileHandler {
    FileStatistics process(Path filePath) throws Exception;
}

class XmlFileHandler implements FileHandler {
    public FileStatistics process(Path filePath) throws Exception {
        FileStatistics stats = new FileStatistics();

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(filePath.toFile());

        NodeList items = document.getElementsByTagName("item");
        for (int i = 0; i < items.getLength(); i++) {
            Element item = (Element) items.item(i);

            String city = item.getAttribute("city");
            String street = item.getAttribute("street");
            String house = item.getAttribute("house");
            String floor = item.getAttribute("floor");

            stats.addRecord(city, street, house, floor);
        }

        return stats;
    }
}

class CsvFileHandler implements FileHandler {
    public FileStatistics process(Path filePath) throws Exception {
        FileStatistics stats = new FileStatistics();

        List<String> lines = Files.readAllLines(filePath);
        for (int i = 1; i < lines.size(); i++) { // Пропускаем заголовок
            String[] parts = lines.get(i).split(";");

            String city = parts[0].trim();
            String street = parts[1].trim();
            String house = parts[2].trim();
            String floor = parts[3].trim();

            stats.addRecord(city, street, house, floor);
        }

        return stats;
    }
}

class FileStatistics {
    private final Map<String, Integer> duplicateRecords = new HashMap<>();
    private final Map<String, int[]> cityStats = new HashMap<>();

    public void addRecord(String city, String street, String house, String floor) {
        String record = city + "," + street + "," + house + "," + floor;
        duplicateRecords.put(record, duplicateRecords.getOrDefault(record, 0) + 1);

        int floorNum = Integer.parseInt(floor);
        cityStats.putIfAbsent(city, new int[5]);
        cityStats.get(city)[floorNum - 1]++;
    }

    public void printStatistics() {
        System.out.println("Дублирующиеся записи:");
        duplicateRecords.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .forEach(entry -> System.out.println(entry.getKey() + " - " + entry.getValue() + " раз(а)"));

        System.out.println("\nСтатистика по городам:");
        cityStats.forEach((city, floors) -> {
            System.out.printf("%s: 1-этажных: %d, 2-этажных: %d, 3-этажных: %d, 4-этажных: %d, 5-этажных: %d%n",
                    city, floors[0], floors[1], floors[2], floors[3], floors[4]);
        });
    }
}

class UnsupportedFileFormatException extends Exception {
    public UnsupportedFileFormatException(String message) {
        super(message);
    }
}
