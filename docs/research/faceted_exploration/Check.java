import com.elicitsoftware.binaryfilter.engine.format.FilterFile;
import com.elicitsoftware.binaryfilter.engine.filter.Session;
import java.nio.file.Path;
import java.util.*;

public class Check {
    static FilterFile.Column col(FilterFile f, String name) {
        for (FilterFile.Column c : f.columns()) if (c.name().equals(name)) return c;
        throw new IllegalArgumentException("no column " + name);
    }
    static int val(FilterFile.Column c, String text) {
        for (int i = 0; i < c.valueCount(); i++) if (!c.isNull(i) && c.value(i).equals(text)) return i;
        throw new IllegalArgumentException("no value " + text + " in " + c.name());
    }
    static String available(Session s, FilterFile.Column c) {
        StringBuilder sb = new StringBuilder();
        for (int v : s.availableValues(c.position()))
            sb.append(c.isNull(v) ? "(null)" : c.value(v)).append('=').append(s.valueCount(c.position(), v)).append(' ');
        return sb.toString().trim();
    }
    static long click(Session s, FilterFile.Column c, String text) {
        long t = System.nanoTime(); s.select(c.position(), val(c, text)); return (System.nanoTime() - t) / 1_000_000;
    }
    public static void main(String[] a) throws Exception {
        Path dir = Path.of(a[0]);
        try (FilterFile rel = FilterFile.open(dir.resolve("relatives.bfilter"))) {
            System.out.printf("relatives: rows=%d columns=%d tabs=%d%n", rel.rowCount(), rel.columns().size(), rel.tabCount());
            for (FilterFile.Column c : rel.columns().subList(0, rel.tabCount()))
                System.out.printf("  tab %-14s %3d values%s%n", c.name(), c.valueCount(), c.hasNull() ? " (+null)" : "");
            Session s = Session.over(rel);
            FilterFile.Column relative = col(rel, "Relative"), gender = col(rel, "Gender"), vital = col(rel, "Vital status");
            System.out.println("  Gender before:        " + available(s, gender));
            long ms = click(s, relative, "Mother");
            System.out.printf("  select Relative=Mother (%d ms): matching=%d%n", ms, s.matchingRowCount());
            System.out.println("  Gender after Mother:  " + available(s, gender));
            System.out.println("  Vital after Mother:   " + available(s, vital));
            ms = click(s, vital, "deceased");
            System.out.printf("  + Vital=deceased (%d ms): matching=%d  (oracle 1042)%n", ms, s.matchingRowCount());
            s.close();
        }
        try (FilterFile dx = FilterFile.open(dir.resolve("diagnoses.bfilter"))) {
            System.out.printf("diagnoses: rows=%d tabs=%d%n", dx.rowCount(), dx.tabCount());
            Session s = Session.over(dx);
            FilterFile.Column relative = col(dx, "Relative"), site = col(dx, "Cancer site"), gender = col(dx, "Gender"),
                              vital = col(dx, "Vital status"), age = col(dx, "Age at diagnosis");
            click(s, relative, "Mother");
            System.out.printf("  Mother: matching=%d (oracle 1125), sites available=%d (oracle 19)%n",
                    s.matchingRowCount(), s.availableValueCount(site.position()));
            s.reset();
            long t = System.nanoTime();
            s.select(gender.position(), val(gender, "female"));
            s.select(vital.position(), val(vital, "alive"));
            s.select(site.position(), val(site, "Breast Cancer"));
            int under50 = 0;
            for (int v = 0; v < age.valueCount(); v++)
                if (!age.isNull(v) && Integer.parseInt(age.value(v)) < 50) { s.select(age.position(), v); under50++; }
            System.out.printf("  female+alive+Breast Cancer+age<50 (%d age values OR'd, %d ms): matching=%d  (oracle 411)%n",
                    under50, (System.nanoTime() - t) / 1_000_000, s.matchingRowCount());
            System.out.println("  Relative tab now:     " + available(s, relative));
            s.close();
        }
        try (FilterFile r = FilterFile.open(dir.resolve("respondents.bfilter"))) {
            System.out.printf("respondents: rows=%d tabs=%d%n", r.rowCount(), r.tabCount());
            Session s = Session.over(r);
            System.out.println("  Status:   " + available(s, col(r, "Status")));
            System.out.println("  Duration: " + available(s, col(r, "Duration")));
            click(s, col(r, "Finalized year"), "2024");
            System.out.println("  2024 -> Finalized month: " + available(s, col(r, "Finalized month")));
            s.close();
        }
    }
}
