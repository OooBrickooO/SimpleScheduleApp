import java.io.File;
import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class ParseXls {
    public static void main(String[] args) throws Exception {
        Workbook workbook = Workbook.getWorkbook(new File("Target/LanZhou_Wyw.xls"));
        Sheet sheet = workbook.getSheet(0);
        for (int i = 0; i < sheet.getRows(); i++) {
            StringBuilder sb = new StringBuilder();
            for (int j = 0; j < sheet.getColumns(); j++) {
                Cell cell = sheet.getCell(j, i);
                sb.append("[").append(cell.getContents().replaceAll("\n", " ")).append("]\t");
            }
            System.out.println(sb.toString());
        }
        workbook.close();
    }
}
