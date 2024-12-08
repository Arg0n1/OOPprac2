import java.util.*;

public class CityDataProcessorApp {
    public static void main(String[] args) {
    }
}

class UserInterface {
    private final FileProcessor fileProcessor;

    public UserInterface(FileProcessor fileProcessor) {
        this.fileProcessor = fileProcessor;
    }

    public void run() {
            while (true) {
                System.out.println("Введите путь до файла-справочника или 'exit' для завершения работы:");

                if ("exit".equalsIgnoreCase(input)) {
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
                } catch (UnsupportedFileFormatException e) {
                    System.out.println(e.getMessage());
                } catch (Exception e) {
                    System.out.println("Произошла ошибка при обработке файла: " + e.getMessage());
                }
            }
    }
}

class FileProcessor {
    public void processFile(Path filePath) throws Exception {
        String fileType = getFileExtension(filePath);

            default -> throw new UnsupportedFileFormatException("Неподдерживаемый формат файла. Поддерживаются только XML и CSV.");
        };

        stats.printStatistics();
    }

    private String getFileExtension(Path filePath) {
        String fileName = filePath.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
    }
}

}





    }

    }
}




    }

    }
}

class FileStatistics {


    }

    public void printStatistics() {
        System.out.println("Дублирующиеся записи:");
        duplicateRecords.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)

        System.out.println("\nСтатистика по городам:");
    }
}

class UnsupportedFileFormatException extends Exception {
    public UnsupportedFileFormatException(String message) {
        super(message);
    }
}