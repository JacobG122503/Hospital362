import objects.Patient;
import objects.PatientVitals;
import java.util.ArrayList;
import java.util.List;

public class AbnormalVitalsAlert {
    public static List<Patient> getFlaggedPatients(List<Patient> patients) {
        List<Patient> flagged = new ArrayList<>();
        for (Patient p : patients) {
            for (PatientVitals v : p.getVitalsHistory()) {
                if (isAbnormal(v, p.getAge())) {
                    flagged.add(p);
                    break;
                }
            }
        }
        return flagged;
    }

    public static PatientVitals getMostRecentAbnormalVitals(Patient p) {
        List<PatientVitals> vitals = p.getVitalsHistory();
        for (int i = vitals.size() - 1; i >= 0; i--) {
            PatientVitals v = vitals.get(i);
            if (isAbnormal(v, p.getAge())) {
                return v;
            }
        }
        return null;
    }

    private static boolean isAbnormal(PatientVitals v, int age) {
        return v.isBloodPressureAbnormal(age) || v.isTemperatureAbnormal(age) || v.isHeartRateAbnormal(age);
    }
}
