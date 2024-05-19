package dashboard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ResourceBundle;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class GeneralDatabasePanel extends JPanel {
    public final DefaultTableModel model;
    public final JTable dataTable;
    private PersonalDatabasePanel personalDatabasePanel;
    private int BookID = 1;
    private final int[] visibleColumns = { 1, 2, 3, 4 };

    public GeneralDatabasePanel(String username, DefaultTableModel model, boolean isAdmin, ResourceBundle messages) {
        this.model = model;
        setLayout(new BorderLayout());

        dataTable = new JTable(model);
        JScrollPane scrollPane = new JScrollPane(dataTable);
        add(scrollPane, BorderLayout.CENTER);
        initializeSorting();

        JPanel buttonPanel = new JPanel();
        JTextField searchField = new JTextField(20);
        JButton searchButton = new JButton(messages.getString("textForSearchButton"));

        if (isAdmin) {
            JButton addButton = new JButton(messages.getString("textForAddButton"));
            buttonPanel.add(addButton);
            addButton.addActionListener(e -> addData(messages));

            JButton deleteButton = new JButton(messages.getString("textForDeleteButton"));
            buttonPanel.add(deleteButton);
            deleteButton.addActionListener(e -> deleteSelectedRow(messages));

            JButton updateButton = new JButton(messages.getString("textForUpdateButton"));
            buttonPanel.add(updateButton);
            updateButton.addActionListener(e -> updateSelectedRow(username, messages));
        } else {
            JButton addToPersonalDatabaseButton = new JButton(messages.getString("textForAddToPersonalDatabaseButton"));
            buttonPanel.add(addToPersonalDatabaseButton);
            addToPersonalDatabaseButton.addActionListener(e -> addToPersonalDatabase(username, messages));
        }

        buttonPanel.add(searchField);
        buttonPanel.add(searchButton);
        add(buttonPanel, BorderLayout.NORTH);

        searchButton.addActionListener(e -> {
            String searchText = searchField.getText().trim().toLowerCase();
            if (!searchText.isEmpty()) {
                TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
                dataTable.setRowSorter(sorter);
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
            } else {
                dataTable.setRowSorter(null);
            }
        });

        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterTable();
            }

            private void filterTable() {
                String searchText = searchField.getText().trim().toLowerCase();
                TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
                dataTable.setRowSorter(sorter);
                if (!searchText.isEmpty()) {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
                } else {
                    dataTable.setRowSorter(null);
                }
            }
        });
    }

    private void initializeSorting() {
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        dataTable.setRowSorter(sorter);

        sorter.setComparator(0, (Object o1, Object o2) -> {
            String title1 = o1.toString();
            String title2 = o2.toString();
            return title1.compareToIgnoreCase(title2);
        });

        sorter.setComparator(1, (Object o1, Object o2) -> {
            String author1 = o1.toString();
            String author2 = o2.toString();
            return author1.compareToIgnoreCase(author2);
        });

        sorter.setComparator(2, (Object o1, Object o2) -> {
            String review1 = o1.toString();
            String review2 = o2.toString();
            return review1.compareToIgnoreCase(review2);
        });
    }

    public void loadCSVData(ResourceBundle messages) {
        try (BufferedReader br = new BufferedReader(new FileReader("generaldatabase/generaldatabase.csv"))) {
            String line;
            boolean skipFirstRow = true; // Flag to skip the first row

            while ((line = br.readLine()) != null) {
                if (skipFirstRow) {
                    skipFirstRow = false; // After skipping the first row, set the flag to false
                    continue; // Skip processing the first row
                }

                if (line.endsWith(",")) {
                    String title = line.substring(0, line.length() - 1).trim();
                    model.addRow(new Object[] { removeQuotationMarks(title), "Unknown", "No Review", "No Rating" });
                } else if (line.startsWith(",")) {
                    String author = line.substring(1).trim();
                    model.addRow(new Object[] { "Unknown", author, "No Review", "No Rating" });
                } else {
                    String[] data = line.split(",");
                    String author = data[data.length - 1].trim();
                    if (author.isEmpty()) {
                        author = "Unknown";
                    }
                    for (int i = 0; i < data.length - 1; i++) {
                        String title = data[i].trim();
                        if (title.isEmpty()) {
                            continue;
                        }
                        model.addRow(new Object[] { removeQuotationMarks(title), author, "No Review", "No Rating" });
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        saveCSVData(messages);
    }

    public void loadCSVDataAfterDatabaseCreated() {
        try (BufferedReader br = new BufferedReader(new FileReader("generaldatabase/generaldatabase.csv"))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",");
                if (data.length >= visibleColumns.length) {
                    Object[] rowData = new Object[visibleColumns.length];
                    for (int i = 0; i < visibleColumns.length; i++) {
                        rowData[i] = data[visibleColumns[i]].trim();
                    }
                    model.addRow(rowData);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void updateSelectedRow(String username, ResourceBundle messages) {

        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, messages.getString("textForUpdateRowWarning"), messages.getString("warningText"), JOptionPane.WARNING_MESSAGE);
            return;
        }

        String bookID = getBookIdFromGeneralDatabase(selectedRow);
        if (bookID == null) {
            JOptionPane.showMessageDialog(this, messages.getString("textForBookIDError"), messages.getString("errorText"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        String[] rowData = new String[dataTable.getColumnCount()];
        for (int i = 0; i < rowData.length; i++) {
            Object value = dataTable.getValueAt(selectedRow, i);
            rowData[i] = value != null ? value.toString() : "";
        }

        JTextField[] textFields = new JTextField[rowData.length];
        for (int i = 0; i < 2; i++) {
            textFields[i] = new JTextField(rowData[i]);
        }

        int option = JOptionPane.showConfirmDialog(this, textFields, "Edit Row", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            for (int i = 0; i < 2; i++) {
                model.setValueAt(textFields[i].getText(), selectedRow, i);
            }

            // Save changes to the CSV files
            saveCSVData(messages);
        }
    }        

    private void saveCSVData(ResourceBundle messages) {
        String filePath = "generaldatabase/generaldatabase.csv";
        try (FileWriter writer = new FileWriter(filePath)) {
            // Write the header
            writer.append("ID,Title,Author,Rating,Review,Status").append("\n");

            // Write data rows
            for (int i = 0; i < model.getRowCount(); i++) {
                // Increment the BookID with each row
                String bookID = Integer.toString(BookID + i);
                writer.append(bookID).append(","); // Write sequential book ID

                for (int j = 0; j < model.getColumnCount(); j++) {
                    String value = model.getValueAt(i, j).toString();
                    writer.append(value);
                    if (j < model.getColumnCount() - 1) {
                        writer.append(",");
                    }
                }
                writer.append("\n");
            }
            BookID = 1;
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, messages.getString("textForSaveCSVDataError"), messages.getString("errorText"), JOptionPane.ERROR_MESSAGE);
        }
    }

    private String removeQuotationMarks(String s) {
        return s.replace("\"", "");
    }

    public void addData(ResourceBundle messages) {
        JTextField titleField = new JTextField(10);
        JTextField authorField = new JTextField(10);

        JPanel inputPanel = new JPanel(new GridLayout(0, 2));
        inputPanel.add(new JLabel("Title:"));
        inputPanel.add(titleField);
        inputPanel.add(new JLabel("Author:"));
        inputPanel.add(authorField);

        int option = JOptionPane.showConfirmDialog(this, inputPanel, "Add New Entry", JOptionPane.OK_CANCEL_OPTION);
        if (option == JOptionPane.OK_OPTION) {
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            if (!title.isEmpty() && !author.isEmpty()) {
                model.addRow(new Object[] { title, author, "No Review", "No Rating" });
                saveCSVData(messages);
            } else {
                JOptionPane.showMessageDialog(this, messages.getString("textForUpdateSelectedRowMethod"), messages.getString("errorText"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void deleteSelectedRow(ResourceBundle messages) {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, messages.getString("textForUserDeleteError"), messages.getString("errorText"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        int confirmDelete = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this row?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);
        if (confirmDelete == JOptionPane.YES_OPTION) {
            model.removeRow(selectedRow);
            saveCSVData(messages);
        }
    }

    public void addToPersonalDatabase(String username, ResourceBundle messages) {
        int selectedRow = dataTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, messages.getString("textForIfUserDontSelectedRowForAddingPersonalDatabase"), messages.getString("errorText"),
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        String bookID = getBookIdFromGeneralDatabase(selectedRow);
        if (bookID == null) {
            JOptionPane.showMessageDialog(this, messages.getString("textForBookIDError"), messages.getString("errorText"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        String title = dataTable.getValueAt(selectedRow, 0).toString(); // Assuming Title is in the first column
        String author = dataTable.getValueAt(selectedRow, 1).toString(); // Assuming Author is in the second column
        String review = dataTable.getValueAt(selectedRow, 2).toString(); // Assuming Review is in the third column
        String rating = dataTable.getValueAt(selectedRow, 3).toString(); // Assuming Rating is in the fourth column

        int confirmAdd = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to add this row to your personal database?", "Confirm Addition",
                JOptionPane.YES_NO_OPTION);
        if (confirmAdd == JOptionPane.YES_OPTION) {
            String personalDatabaseFilePath = "userdatabases/" + username + ".csv";
            try (FileWriter writer = new FileWriter(personalDatabaseFilePath, true)) {
                writer.append(bookID).append(",");
                writer.append(title).append(",");
                writer.append(author).append(",");
                writer.append(review).append(",");
                writer.append(rating).append(",");
                writer.append("Not Started").append("\n");

                JOptionPane.showMessageDialog(this, messages.getString("textAfterUserAddingBookToPersonalDatabase"), messages.getString("successText"),
                        JOptionPane.INFORMATION_MESSAGE);

                personalDatabasePanel = new PersonalDatabasePanel(model, username, messages);
                personalDatabasePanel.loadPersonalCsvData(username);
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, messages.getString("textForErrorWhileAddingToPersonalDatabase"), messages.getString("errorText"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String getBookIdFromGeneralDatabase(int selectedRow) {
        String bookID = null;
        try (BufferedReader br = new BufferedReader(new FileReader("generaldatabase/generaldatabase.csv"))) {
            String line;
            int currentRow = 0;

            while ((line = br.readLine()) != null) {
                if (currentRow == selectedRow) {
                    String[] data = line.split(",");
                    if (data.length > 0) {
                        bookID = data[0].trim();
                        break;
                    }
                }
                currentRow++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bookID;
    }
}