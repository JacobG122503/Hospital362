package services;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import objects.Employee;
import objects.Patient;

public class PatientsService {
    private static String patientIDTag = "P";
    public static void createService(
            Scanner scanner,
            List<Patient> patients,
            Runnable onSave
    )
    {
        System.out.print("\033[H\033[2J\033[3J");
        System.out.flush();
        System.out.println("\n  === Patient Repository ===\n");
        System.out.println("  [1] New Patient");
        System.out.println("  [2] View All Patients");
        System.out.println("  [3] Search Patient");
        System.out.print("\n  Select type: ");
        String type = scanner.nextLine().trim();

        if(type.equals(("1")))
        {
            System.out.println("Input the Patient Information below to Admit the Patient");
            System.out.println("Admitting Patient...");

            System.out.print("  Name: ");
            String name = scanner.nextLine().trim();
            System.out.print("  Age: ");
            int age = Integer.parseInt(scanner.nextLine().trim());
            System.out.print("  Gender: ");
            String gender = scanner.nextLine().trim();
            System.out.print("  Phone: ");
            String phone = scanner.nextLine().trim();
            System.out.print("  Address: ");
            String address = scanner.nextLine().trim();
            System.out.print("  Diagnosis: ");
            String diag = scanner.nextLine().trim();
            System.out.print("  Room Number: ");
            String room = scanner.nextLine().trim();
            System.out.print("  Admission Date (YYYY-MM-DD): ");
            String admDate = scanner.nextLine().trim();
            System.out.print("  Insurance Provider: ");
            String ins = scanner.nextLine().trim();

            patients.add(new Patient(name, age, gender, phone, address, patientIDTag + (patients.size()  + 1), diag, room, admDate, ins));
            onSave.run();
            System.out.println("\n  Patient created successfully!");

        }
        else if(type.equals(("2")))
        {
            System.out.print("\033[H\033[2J\033[3J");
            System.out.flush();
            System.out.println("\n  === All Patients ===\n");

            if (patients.isEmpty()) {
                System.out.println("  No patients in the system.");
            } else {
                for (int i = 0; i < patients.size(); i++) {
                    Patient p = patients.get(i);
                    System.out.println("  " + (i + 1) + ". " + p.getName()
                            + " | ID: " + p.getPatientId()
                            + " | Room: " + p.getRoomNumber()
                            + " | Diagnosis: " + p.getDiagnosis());
                }
            }

            System.out.println("\n  Press Enter to return to menu...");
            scanner.nextLine();
        }
        else if(type.equals(("3")))
        {
            System.out.print("  Name: ");
            String patientName = scanner.nextLine().trim();
            List<Patient> similarNames = new ArrayList<Patient>();
            for(Patient p : patients)
            {
                if(p.getName().contains(patientName))
                {
                    similarNames.add(p);
                }
            }
            if(similarNames.isEmpty())
            {
                System.out.println("No patient found with that name");
            }
            for(Patient p : similarNames)
            {
                System.out.println(p.toString());
            }
        }
        else
        {
            System.out.println("\n  Invalid selection.");
        }
        System.out.println("  Press Enter to return to menu...");
        scanner.nextLine();
    }
}
