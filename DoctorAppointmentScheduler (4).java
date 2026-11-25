import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Scanner;

/*
 * PS 51 - Doctor Appointment Scheduler
 * Language : Java (single .java file, run from the console)
 * Concepts : ArrayList, HashMap, Searching, Sorting, Methods, File Handling
 *
 * How to run:
 *   javac DoctorAppointmentScheduler.java
 *   java DoctorAppointmentScheduler
 *
 * Data is stored in scheduler_data.txt (one record per line):
 *   D,DoctorID,Name,Specialization         e.g. D,DOC101,Amit Rao,Cardiology
 *   A,AppointmentID,DoctorID,PatientName,Slot   e.g. A,APT1001,DOC101,Ravi Kumar,2
 */

// ---------------------------------------------------------------------------
// DoctorAppointmentScheduler : menu (main class)
// ---------------------------------------------------------------------------
public class DoctorAppointmentScheduler {

    private static final Scanner sc = new Scanner(System.in);
    private static final SchedulerManager manager = new SchedulerManager();

    public static void main(String[] args) {
        manager.loadFromFile();
        boolean running = true;
        while (running) {
            showMenu();
            String line = readLine("Enter choice: ");
            if (line == null) {          // input ended (for example Ctrl+D) -> save and quit
                break;
            }
            switch (readChoice(line)) {
                case 1: addDoctor(); break;
                case 2: bookAppointment(); break;
                case 3: searchDoctor(); break;
                case 4: manager.generateReport(); break;
                case 5: running = false; break;
                default:
                    System.out.println("Invalid choice. Enter a number from 1 to 5.");
            }
        }
        manager.saveToFile();
        System.out.println("Data saved to scheduler_data.txt. Goodbye!");
    }

    private static void showMenu() {
        System.out.println();
        System.out.println("========== DOCTOR APPOINTMENT SCHEDULER ==========");
        System.out.println("1. Add Doctor");
        System.out.println("2. Book Appointment (slot 1-8)");
        System.out.println("3. Search Doctor");
        System.out.println("4. Appointment Report");
        System.out.println("5. Exit");
    }

    private static int readChoice(String line) {
        try {
            return Integer.parseInt(line.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    // prints a prompt and returns the typed line (null if there is no more input)
    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!sc.hasNextLine()) {
            System.out.println();
            return null;
        }
        return sc.nextLine();
    }

    // ---------------- menu actions ----------------

    private static void addDoctor() {
        String name = readLine("Enter doctor name: ");
        if (name == null) return;
        name = name.trim();
        if (name.isEmpty()) {
            System.out.println("Doctor name cannot be empty.");
            return;
        }
        if (name.contains(",")) {
            System.out.println("Doctor name cannot contain a comma.");
            return;
        }
        String spec = readLine("Enter specialization: ");
        if (spec == null) return;
        spec = spec.trim();
        if (spec.isEmpty()) {
            System.out.println("Specialization cannot be empty.");
            return;
        }
        if (spec.contains(",")) {
            System.out.println("Specialization cannot contain a comma.");
            return;
        }
        Doctor d = manager.addDoctor(name, spec);
        System.out.println("Doctor added. " + manager.describeDoctor(d));
    }

    private static void bookAppointment() {
        String id = readLine("Enter doctor ID: ");
        if (id == null) return;
        String patient = readLine("Enter patient name: ");
        if (patient == null) return;
        String text = readLine("Enter slot (1-8, 1 = 09:00 ... 8 = 16:00): ");
        if (text == null) return;

        patient = patient.trim();
        if (patient.isEmpty()) {
            System.out.println("Patient name cannot be empty.");
            return;
        }
        if (patient.contains(",")) {
            System.out.println("Patient name cannot contain a comma.");
            return;
        }
        int slot;
        try {
            slot = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid slot. Enter a number from 1 to 8.");
            return;
        }
        if (slot < 1 || slot > SchedulerManager.SLOTS) {
            System.out.println("Invalid slot. Enter a number from 1 to 8.");
            return;
        }
        Doctor d = manager.searchDoctorById(id);
        if (d == null) {
            System.out.println("Doctor not found.");
            return;
        }
        if (manager.isSlotBooked(d.getId(), slot)) {
            System.out.println("Slot already booked. Choose another slot.");
            return;
        }
        Appointment a = manager.bookAppointment(d, patient, slot);
        System.out.println("Appointment booked. " + manager.describeAppointment(a));
    }

    private static void searchDoctor() {
        String key = readLine("Enter doctor ID, name or specialization: ");
        if (key == null) return;
        if (key.trim().isEmpty()) {
            System.out.println("Please enter an ID, a name or a specialization.");
            return;
        }
        Doctor d = manager.searchDoctor(key);
        if (d == null) {
            System.out.println("Doctor not found.");
        } else {
            System.out.println("Found: " + manager.describeDoctor(d));
            manager.printSchedule(d);
        }
    }
}

// ---------------------------------------------------------------------------
// Doctor : one doctor record
// ---------------------------------------------------------------------------
class Doctor {
    private final String id;
    private final String name;
    private final String specialization;

    Doctor(String id, String name, String specialization) {
        this.id = id;
        this.name = name;
        this.specialization = specialization;
    }

    String getId() { return id; }
    String getName() { return name; }
    String getSpecialization() { return specialization; }

    // line stored in scheduler_data.txt
    String toFileString() {
        return "D," + id + "," + name + "," + specialization;
    }
}

// ---------------------------------------------------------------------------
// Appointment : one booked slot of one doctor
// ---------------------------------------------------------------------------
class Appointment {
    private final String id;
    private final String doctorId;
    private final String patientName;
    private final int slot;              // 1 to 8

    Appointment(String id, String doctorId, String patientName, int slot) {
        this.id = id;
        this.doctorId = doctorId;
        this.patientName = patientName;
        this.slot = slot;
    }

    String getId() { return id; }
    String getDoctorId() { return doctorId; }
    String getPatientName() { return patientName; }
    int getSlot() { return slot; }

    // line stored in scheduler_data.txt
    String toFileString() {
        return "A," + id + "," + doctorId + "," + patientName + "," + slot;
    }
}

// ---------------------------------------------------------------------------
// SchedulerManager : ArrayList + HashMap + file handling + searching/sorting
// ---------------------------------------------------------------------------
class SchedulerManager {

    static final int SLOTS = 8;                          // slot 1 = 09:00 ... slot 8 = 16:00
    private static final String FILE_NAME = "scheduler_data.txt";
    private static final int FIRST_DOCTOR_ID = 101;
    private static final int FIRST_APPOINTMENT_ID = 1001;

    // true  -> report is sorted by doctor ID and slot with our own sort
    // false -> report is printed in booking order
    private static final boolean SORT_REPORT = true;

    private final ArrayList<Doctor> doctorList = new ArrayList<>();
    private final HashMap<String, Doctor> doctorMap = new HashMap<>();
    private final ArrayList<Appointment> appointmentList = new ArrayList<>();
    private final HashMap<String, Appointment> slotMap = new HashMap<>();   // "DOC101-2" -> appointment

    private int nextDoctorId = FIRST_DOCTOR_ID;
    private int nextAppointmentId = FIRST_APPOINTMENT_ID;

    // ---------------- file handling ----------------

    // Load data from the file at start. A missing file just means "no data yet".
    void loadFromFile() {
        File file = new File(FILE_NAME);
        if (!file.exists()) {
            return;
        }
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }
                String[] parts = line.split(",");
                try {
                    if (parts[0].equals("D") && parts.length == 4) {
                        loadDoctor(parts);
                    } else if (parts[0].equals("A") && parts.length == 5) {
                        loadAppointment(parts);
                    }
                    // any other line is damaged and is skipped
                } catch (NumberFormatException ex) {
                    // skip a line with bad numbers
                }
            }
        } catch (IOException ex) {
            System.out.println("Could not read " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    private void loadDoctor(String[] parts) {
        String id = parts[1].trim().toUpperCase();
        String name = parts[2].trim();
        String spec = parts[3].trim();
        if (!id.startsWith("DOC") || doctorMap.containsKey(id) || name.isEmpty() || spec.isEmpty()) {
            return;
        }
        int number = Integer.parseInt(id.substring(3));
        Doctor d = new Doctor(id, name, spec);
        doctorList.add(d);
        doctorMap.put(id, d);
        if (number >= nextDoctorId) {
            nextDoctorId = number + 1;
        }
    }

    private void loadAppointment(String[] parts) {
        String id = parts[1].trim().toUpperCase();
        String doctorId = parts[2].trim().toUpperCase();
        String patient = parts[3].trim();
        int slot = Integer.parseInt(parts[4].trim());
        if (!id.startsWith("APT") || patient.isEmpty() || !doctorMap.containsKey(doctorId)
                || slot < 1 || slot > SLOTS || slotMap.containsKey(slotKey(doctorId, slot))) {
            return;
        }
        int number = Integer.parseInt(id.substring(3));
        Appointment a = new Appointment(id, doctorId, patient, slot);
        appointmentList.add(a);
        slotMap.put(slotKey(doctorId, slot), a);
        if (number >= nextAppointmentId) {
            nextAppointmentId = number + 1;
        }
    }

    // Save every doctor and every appointment to the file (called on exit).
    void saveToFile() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_NAME))) {
            for (int i = 0; i < doctorList.size(); i++) {
                bw.write(doctorList.get(i).toFileString());
                bw.newLine();
            }
            for (int i = 0; i < appointmentList.size(); i++) {
                bw.write(appointmentList.get(i).toFileString());
                bw.newLine();
            }
        } catch (IOException ex) {
            System.out.println("Could not save " + FILE_NAME + ": " + ex.getMessage());
        }
    }

    // ---------------- features ----------------

    // 1 - Add Doctor
    Doctor addDoctor(String name, String specialization) {
        String id = "DOC" + nextDoctorId;
        nextDoctorId++;
        Doctor d = new Doctor(id, name, specialization);
        doctorList.add(d);          // ArrayList keeps the order
        doctorMap.put(id, d);       // HashMap gives quick lookup by ID
        return d;
    }

    // 2 - Book Appointment. The caller has already checked the doctor, the patient name,
    //     the slot number (1 to 8) and that the slot is free.
    Appointment bookAppointment(Doctor d, String patient, int slot) {
        String id = "APT" + nextAppointmentId;
        nextAppointmentId++;
        Appointment a = new Appointment(id, d.getId(), patient, slot);
        appointmentList.add(a);
        slotMap.put(slotKey(d.getId(), slot), a);   // HashMap makes the "is it free?" check O(1)
        return a;
    }

    // true if the doctor already has an appointment in this slot, O(1) on average
    boolean isSlotBooked(String doctorId, int slot) {
        return slotMap.containsKey(slotKey(doctorId, slot));
    }

    // 3a - Search by ID : HashMap lookup, O(1) on average
    Doctor searchDoctorById(String id) {
        return doctorMap.get(id.trim().toUpperCase());
    }

    // 3b - Search by name or specialization : our own linear search over the list, O(n)
    Doctor searchByNameOrSpecialization(String text) {
        String key = text.trim().toLowerCase();
        // 1. exact name
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            if (d.getName().toLowerCase().equals(key)) {
                return d;
            }
        }
        // 2. part of a name (for example "neha")
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            if (d.getName().toLowerCase().contains(key)) {
                return d;
            }
        }
        // 3. part of a specialization (for example "cardio")
        for (int i = 0; i < doctorList.size(); i++) {
            Doctor d = doctorList.get(i);
            if (d.getSpecialization().toLowerCase().contains(key)) {
                return d;
            }
        }
        return null;
    }

    // Search by ID first, otherwise by name / specialization
    Doctor searchDoctor(String key) {
        Doctor d = searchDoctorById(key);
        if (d == null) {
            d = searchByNameOrSpecialization(key);
        }
        return d;
    }

    // 4 - Appointment Report
    void generateReport() {
        if (appointmentList.isEmpty()) {
            System.out.println("No appointments booked yet.");
            return;
        }
        // work on a copy so the original (booking) order in the file never changes
        ArrayList<Appointment> view = new ArrayList<>(appointmentList);
        if (SORT_REPORT) {
            sortAppointments(view);
        }
        System.out.println("-------------- APPOINTMENT REPORT --------------");
        for (int i = 0; i < view.size(); i++) {
            System.out.println(describeAppointment(view.get(i)));
        }
        System.out.println("------------------------------------------------");
        System.out.println("Total appointments: " + view.size());
    }

    // ---------------- helpers ----------------

    // Own selection sort (no Collections.sort / Arrays.sort):
    // lower doctor ID first, and for the same doctor the earlier slot first.
    private void sortAppointments(ArrayList<Appointment> list) {
        for (int i = 0; i < list.size() - 1; i++) {
            int best = i;
            for (int j = i + 1; j < list.size(); j++) {
                if (comesBefore(list.get(j), list.get(best))) {
                    best = j;
                }
            }
            if (best != i) {
                Appointment temp = list.get(i);
                list.set(i, list.get(best));
                list.set(best, temp);
            }
        }
    }

    // true if a should be listed before b
    private boolean comesBefore(Appointment a, Appointment b) {
        int da = doctorNumber(a.getDoctorId());
        int db = doctorNumber(b.getDoctorId());
        if (da != db) {
            return da < db;
        }
        return a.getSlot() < b.getSlot();
    }

    private int doctorNumber(String doctorId) {
        return Integer.parseInt(doctorId.substring(3));
    }

    private String slotKey(String doctorId, int slot) {
        return doctorId + "-" + slot;
    }

    // slot 1 = 09:00, slot 2 = 10:00 ... slot 8 = 16:00
    static String slotTime(int slot) {
        return String.format(Locale.US, "%02d:00", 8 + slot);
    }

    // One-line description of a doctor
    String describeDoctor(Doctor d) {
        return d.getId() + " | " + d.getName() + " | " + d.getSpecialization();
    }

    // One-line description of an appointment
    String describeAppointment(Appointment a) {
        Doctor d = doctorMap.get(a.getDoctorId());
        return a.getId() + " | " + d.getId() + " | " + d.getName()
                + " | " + slotTime(a.getSlot()) + " | Patient: " + a.getPatientName();
    }

    // Booked count, free slots and the appointments of one doctor (slots 1..8 in order)
    void printSchedule(Doctor d) {
        int booked = 0;
        StringBuilder free = new StringBuilder();
        for (int slot = 1; slot <= SLOTS; slot++) {
            if (isSlotBooked(d.getId(), slot)) {
                booked++;
            } else {
                if (free.length() > 0) {
                    free.append(", ");
                }
                free.append(slotTime(slot));
            }
        }
        if (free.length() == 0) {
            free.append("none");
        }
        System.out.println("Booked: " + booked + " of " + SLOTS + " slots | Free: " + free);
        if (booked == 0) {
            System.out.println("No appointments booked.");
            return;
        }
        System.out.println("Appointments:");
        for (int slot = 1; slot <= SLOTS; slot++) {
            Appointment a = slotMap.get(slotKey(d.getId(), slot));
            if (a != null) {
                System.out.println("  " + slotTime(slot) + " | " + a.getId()
                        + " | Patient: " + a.getPatientName());
            }
        }
    }
}
