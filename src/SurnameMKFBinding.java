import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SurnameMKFBinding {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private List<String> mkfList;
    private HashMap<String, List<String>> bonds;
    private String filePath = "C:\\Users\\Zbiga\\Downloads\\mi\\MKFBonds.txt";

    public SurnameMKFBinding() {
        mkfList = IntStream.rangeClosed(1, 900)
                .mapToObj(i -> "MKF" + i)
                .collect(Collectors.toList());
        bonds = new HashMap<>();
        initializeUI();
        SwingUtilities.invokeLater(this::promptForDataLoading);
    }

    private void initializeUI() {
        frame = new JFrame("Surname MKF Binding");
        model = new DefaultTableModel();
        model.addColumn("Surnames");
        model.addColumn("Bonds");

        table = new JTable(model);
        JButton bindButton = new JButton("Bind");
        JButton findButton = new JButton("Find");
        JButton deleteButton = new JButton("Delete");

        bindButton.addActionListener(e -> bindAction());
        findButton.addActionListener(e -> findAction(e));
        deleteButton.addActionListener(e -> deleteAction(e));

        frame.setLayout(new BorderLayout());
        frame.add(new JScrollPane(table), BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(bindButton);
        buttonPanel.add(findButton);
        buttonPanel.add(deleteButton);
        frame.add(buttonPanel, BorderLayout.SOUTH);

        frame.setSize(800, 600);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                confirmAndExit();
            }
        });
        frame.setVisible(true);
    }

    private void confirmAndExit() {
        int result = JOptionPane.showConfirmDialog(frame, "Do you want to save changes?", "Save Changes", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            writeToFile();
        }
        System.exit(0);
    }

    private void promptForDataLoading() {
        int result = JOptionPane.showConfirmDialog(frame, "Should we load data from file?", "Load Data", JOptionPane.YES_NO_OPTION);
        if (result == JOptionPane.YES_OPTION) {
            loadDataFromFile();
        }
    }

    private void loadDataFromFile() {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            model.setRowCount(0); // Clear existing data
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(" - Bonds: ");
                if (parts.length == 2) {
                    String surname = parts[0].replace("Surname: ", "").trim();
                    List<String> bondList = Arrays.asList(parts[1].split(",\\s*"));
                    bonds.put(surname, new ArrayList<>(bondList));
                    mkfList.removeAll(bondList);
                    model.addRow(new Object[]{surname, String.join(", ", bondList)});
                } else if (parts.length == 1) {
                    String surname = parts[0].replace("Surname: ", "").trim();
                    bonds.put(surname, new ArrayList<>());
                    model.addRow(new Object[]{surname, ""});
                }
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to load data from file: " + e.getMessage());
        }
    }

    private void bindAction() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select a surname row to bind.");
            return;
        }

        String surname = model.getValueAt(selectedRow, 0).toString();
        List<String> availableMKFs = new ArrayList<>(mkfList); // Show all MKFs not currently bonded
        String selectedMKF = (String) JOptionPane.showInputDialog(frame, "Choose MKF to bind:",
                "Select MKF", JOptionPane.QUESTION_MESSAGE, null, availableMKFs.toArray(), availableMKFs.isEmpty() ? null : availableMKFs.get(0));

        if (selectedMKF != null) {
            List<String> bondedMKFs = bonds.getOrDefault(surname, new ArrayList<>());
            if (!bondedMKFs.contains(selectedMKF)) {
                bondedMKFs.add(selectedMKF);
                bonds.put(surname, bondedMKFs);
                mkfList.remove(selectedMKF);
                Collections.sort(bondedMKFs, Comparator.comparingInt(SurnameMKFBinding::extractMKFNumber));
                updateTable();
            }
        }
    }

    private void findAction(ActionEvent e) {
        String mkfToFind = JOptionPane.showInputDialog(frame, "Which MKF would you like to find?");
        if (mkfToFind != null && !mkfToFind.trim().isEmpty()) {
            boolean found = false;
            for (int i = 0; i < model.getRowCount(); i++) {
                List<String> bondList = bonds.getOrDefault(model.getValueAt(i, 0).toString(), new ArrayList<>());
                if (bondList.contains(mkfToFind)) {
                    table.setRowSelectionInterval(i, i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                JOptionPane.showMessageDialog(frame, "MKF not chosen by customer.");
            }
        }
    }

    private void deleteAction(ActionEvent e) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(frame, "Please select a row from which to delete MKF bindings.");
            return;
        }

        String surname = model.getValueAt(selectedRow, 0).toString();
        List<String> bondedMKFs = bonds.getOrDefault(surname, new ArrayList<>());
        if (bondedMKFs.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No MKF bindings to delete for selected surname.");
            return;
        }

        Object[] options = bondedMKFs.toArray();
        String mkfToDelete = (String) JOptionPane.showInputDialog(frame, "Select MKF to unbind from " + surname,
                "Unbind MKF", JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        if (mkfToDelete != null) {
            bondedMKFs.remove(mkfToDelete);
            mkfList.add(mkfToDelete);
            Collections.sort(mkfList, Comparator.comparingInt(SurnameMKFBinding::extractMKFNumber));
            updateTable();
        }
    }

    private void updateTable() {
        for (int i = 0; i < model.getRowCount(); i++) {
            String surname = model.getValueAt(i, 0).toString();
            List<String> bondedMKFs = bonds.getOrDefault(surname, new ArrayList<>());
            model.setValueAt(String.join(", ", bondedMKFs), i, 1);
        }
    }

    private void writeToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String surname : bonds.keySet()) {
                writer.write("Surname: " + surname + " - Bonds: " + String.join(", ", bonds.get(surname)));
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(frame, "Failed to write to file: " + e.getMessage());
        }
    }

    private static int extractMKFNumber(String mkf) {
        return Integer.parseInt(mkf.substring(3));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SurnameMKFBinding::new);
    }
}
