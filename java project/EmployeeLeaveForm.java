import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Window;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;



public class EmployeeLeaveForm {

    private static final String RECORDS_FILE = "leave-records.dat";
    private static final String EMPLOYEES_FILE = "employees.dat";
    private static final List<LeaveRecord> RECORDS = new ArrayList<>();
    private static final List<EmployeeRecord> EMPLOYEES = new ArrayList<>();
    private static final Color COLOR_PRIMARY = new Color(41, 64, 82);
    private static final Color COLOR_SUCCESS = new Color(46, 125, 50);
    private static final Color COLOR_WARNING = new Color(156, 117, 22);
    private static final Color COLOR_DANGER = new Color(183, 28, 28);
    private static final Color COLOR_NEUTRAL = new Color(69, 90, 100);
    private static final int YEARLY_NON_DEDUCTED_LEAVE_DAYS = 12;
    private static final int CONTINUOUS_LEAVE_LIMIT_DAYS = 5;
    private static final int SALARY_DEDUCTION_PER_DAY = 2000;
    private static final String MYSQL_URL = "jdbc:mysql://localhost:3306/jdbcdb";
    private static final String MYSQL_USER = "root";
    private static final String MYSQL_PASSWORD = "root";
    private static final String BACKGROUND_IMAGE_PATH = "leave";
    private static final Image BACKGROUND_IMAGE = loadBackgroundImage();

    public static void main(String[] args) {
        loadEmployees();
        loadRecords();
        if (args != null && args.length > 0 && "--list-employees".equalsIgnoreCase(args[0])) {
            printEmployeesToConsole();
            return;
        }
        if (args != null && args.length > 0 && "--sync-employees-mysql".equalsIgnoreCase(args[0])) {
            syncEmployeesToMySql();
            return;
        }
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }

    private static void styleButton(JButton button, Color backgroundColor) {
        button.setBackground(backgroundColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
    }

    private static Image loadBackgroundImage() {
        File imageFile = new File(BACKGROUND_IMAGE_PATH);
        if (!imageFile.exists()) {
            return null;
        }
        try {
            return ImageIO.read(imageFile);
        } catch (IOException ex) {
            return null;
        }
    }

    private static void applyWindowBackground(JFrame frame) {
        frame.setContentPane(new BackgroundPanel());
        frame.setLayout(new BorderLayout());
    }

    private static void applyPlainWhiteBackground(JFrame frame) {
        frame.getContentPane().setBackground(Color.WHITE);
        frame.setLayout(new BorderLayout());
    }

    private static void makePanelTransparent(JPanel panel) {
        panel.setOpaque(false);
    }

    private static void makeScrollPaneTransparent(JScrollPane scrollPane) {
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
    }

    private static class BackgroundPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            if (BACKGROUND_IMAGE != null) {
                g2.drawImage(BACKGROUND_IMAGE, 0, 0, getWidth(), getHeight(), this);
                g2.setColor(new Color(0, 0, 0, 95));
                g2.fillRect(0, 0, getWidth(), getHeight());
            } else {
                GradientPaint gradient = new GradientPaint(
                        0, 0, new Color(235, 245, 255),
                        0, getHeight(), new Color(210, 226, 239)
                );
                g2.setPaint(gradient);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
            g2.dispose();
        }
    }

    private static void logoutAndShowLogin() {
        for (Window window : Window.getWindows()) {
            if (window.isDisplayable()) {
                window.dispose();
            }
        }
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }

    // -------------------- DATA MODEL --------------------
    private static class EmployeeRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        final int employeeId;
        final String employeeName;
        final String emailId;
        final String password;
        final String phoneNumber;
        final String department;
        final String role;
        final LocalDate dateOfBirth;
        final String gender;
        final LocalDate dateOfJoining;

        EmployeeRecord(int employeeId, String employeeName, String emailId, String password,
                       String phoneNumber, String department, String role,
                       LocalDate dateOfBirth, String gender, LocalDate dateOfJoining) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.emailId = emailId;
            this.password = password;
            this.phoneNumber = phoneNumber;
            this.department = department;
            this.role = role;
            this.dateOfBirth = dateOfBirth;
            this.gender = gender;
            this.dateOfJoining = dateOfJoining;
        }
    }

    private static class LeaveRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        final int employeeId;
        final String employeeName;
        final String leaveType;
        final int durationDays;
        final LocalDate fromDate;
        final LocalDate toDate;
        final String requestedBy;
        final LocalDate requestedOn;
        String status;

        LeaveRecord(int employeeId, String employeeName, String leaveType, int durationDays,
                    LocalDate fromDate, LocalDate toDate, String requestedBy,
                    LocalDate requestedOn, String status) {
            this.employeeId = employeeId;
            this.employeeName = employeeName;
            this.leaveType = leaveType;
            this.durationDays = durationDays;
            this.fromDate = fromDate;
            this.toDate = toDate;
            this.requestedBy = requestedBy;
            this.requestedOn = requestedOn;
            this.status = status;
        }
    }

    // -------------------- LOGIN WINDOW --------------------
    private static class LoginWindow extends JFrame {
        private final JTextField usernameField = new JTextField(14);
        private final JPasswordField passwordField = new JPasswordField(14);
        private final JButton togglePasswordButton = new JButton("\uD83D\uDC41");
        private boolean passwordVisible = false;
        private char defaultEchoChar;

        LoginWindow() {
            setTitle("Login - Employee Leave System");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyWindowBackground(this);

            JPanel form = new JPanel(new GridBagLayout());
            makePanelTransparent(form);
            form.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            JLabel header = new JLabel("Authorized Access Only", JLabel.CENTER);
            header.setFont(new Font("Segoe UI", Font.BOLD, 22));
            form.add(header, gbc);

            gbc.gridwidth = 1;
            gbc.gridx = 0;
            gbc.gridy = 1;
            JLabel usernameLabel = new JLabel("Username");
            usernameLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            form.add(usernameLabel, gbc);
            gbc.gridx = 1;
            usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            usernameField.setPreferredSize(new Dimension(330, 34));
            form.add(usernameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            JLabel passwordLabel = new JLabel("Password");
            passwordLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
            form.add(passwordLabel, gbc);
            gbc.gridx = 1;
            JPanel passwordRow = new JPanel(new BorderLayout(6, 0));
            makePanelTransparent(passwordRow);
            passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
            passwordField.setPreferredSize(new Dimension(330, 34));
            passwordRow.add(passwordField, BorderLayout.CENTER);
            togglePasswordButton.setFocusable(false);
            togglePasswordButton.setMargin(new Insets(2, 6, 2, 6));
            togglePasswordButton.setToolTipText("Show/Hide Password");
            togglePasswordButton.addActionListener(e -> togglePasswordVisibility());
            passwordRow.add(togglePasswordButton, BorderLayout.EAST);
            form.add(passwordRow, gbc);

            JButton loginButton = new JButton("Login");
            styleButton(loginButton, COLOR_PRIMARY);
            loginButton.setFont(new Font("Segoe UI", Font.BOLD, 15));
            loginButton.setPreferredSize(new Dimension(100, 32));
            loginButton.addActionListener(e -> attemptLogin());
            gbc.gridx = 1;
            gbc.gridy = 3;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.CENTER;
            form.add(loginButton, gbc);

            add(form, BorderLayout.CENTER);
            defaultEchoChar = passwordField.getEchoChar();
        }

        private void togglePasswordVisibility() {
            passwordVisible = !passwordVisible;
            if (passwordVisible) {
                passwordField.setEchoChar((char) 0);
            } else {
                passwordField.setEchoChar(defaultEchoChar);
            }
        }

        private void attemptLogin() {
            String username = usernameField.getText().trim();
            String password = new String(passwordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter username and password.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            UserSession session = UserSession.authenticate(username, password);
            if (session == null) {
                JOptionPane.showMessageDialog(this, "Invalid credentials.", "Access Denied",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            new MainWindow(session).setVisible(true);
            dispose();
        }
    }

    // -------------------- MAIN WINDOW --------------------
    private static class MainWindow extends JFrame {
        private final UserSession session;

        MainWindow(UserSession session) {
            this.session = session;
            setTitle("Main - Employee Leave System");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyWindowBackground(this);

            JLabel header = new JLabel("Welcome, " + session.username, JLabel.CENTER);
            header.setFont(new Font("Segoe UI", Font.BOLD, 18));
            add(header, BorderLayout.NORTH);

            JPanel buttons = new JPanel();
            makePanelTransparent(buttons);
            buttons.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
            buttons.setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.ipadx = 20;
            gbc.ipady = 10;

            JButton applyButton = new JButton("Apply Leave");
            styleButton(applyButton, COLOR_SUCCESS);
            applyButton.addActionListener(e -> new ApplyLeaveWindow(session).setVisible(true));
            if (!session.isAdmin) {
                buttons.add(applyButton, gbc);
                gbc.gridy++;

                JButton profileButton = new JButton("My Profile");
                styleButton(profileButton, COLOR_PRIMARY);
                profileButton.addActionListener(e -> new MyProfileWindow(session).setVisible(true));
                buttons.add(profileButton, gbc);
                gbc.gridy++;
            }

            JButton approveButton = new JButton("Approve/Reject Leave");
            styleButton(approveButton, COLOR_WARNING);
            approveButton.addActionListener(e -> new ApproveLeaveWindow(session).setVisible(true));
            if (session.isAdmin) {
                buttons.add(approveButton, gbc);
                gbc.gridy++;
            }

            JButton viewButton = new JButton("View Leave Records");
            styleButton(viewButton, COLOR_PRIMARY);
            viewButton.addActionListener(e -> new ViewLeaveRecordsWindow(session).setVisible(true));
            buttons.add(viewButton, gbc);

            JButton searchButton = new JButton("Search Leave Records");
            styleButton(searchButton, COLOR_NEUTRAL);
            searchButton.addActionListener(e -> new SearchLeaveWindow().setVisible(true));
            if (session.isAdmin) {
                gbc.gridy++;
                buttons.add(searchButton, gbc);

                JButton addEmployeeButton = new JButton("Add Employee");
                styleButton(addEmployeeButton, COLOR_SUCCESS);
                addEmployeeButton.addActionListener(e -> new AddEmployeeWindow().setVisible(true));
                gbc.gridy++;
                buttons.add(addEmployeeButton, gbc);

                JButton removeEmployeeButton = new JButton("Remove Employee");
                styleButton(removeEmployeeButton, COLOR_DANGER);
                removeEmployeeButton.addActionListener(e -> new RemoveEmployeeWindow().setVisible(true));
                gbc.gridy++;
                buttons.add(removeEmployeeButton, gbc);

                JButton updateEmployeeButton = new JButton("Update Employee");
                styleButton(updateEmployeeButton, COLOR_WARNING);
                updateEmployeeButton.addActionListener(e -> new UpdateEmployeeWindow().setVisible(true));
                gbc.gridy++;
                buttons.add(updateEmployeeButton, gbc);

                JButton viewEmployeesButton = new JButton("View Employees");
                styleButton(viewEmployeesButton, COLOR_PRIMARY);
                viewEmployeesButton.addActionListener(e -> new ViewEmployeesWindow().setVisible(true));
                gbc.gridy++;
                buttons.add(viewEmployeesButton, gbc);
            }

            add(buttons, BorderLayout.CENTER);

            JButton logoutButton = new JButton("Logout");
            styleButton(logoutButton, COLOR_PRIMARY);
            logoutButton.addActionListener(e -> logoutAndShowLogin());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(logoutButton);
            add(footer, BorderLayout.SOUTH);
        }
    }

    private static class MyProfileWindow extends JFrame {
        private final UserSession session;

        MyProfileWindow(UserSession session) {
            this.session = session;
            setTitle("My Profile");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            EmployeeRecord employee = findEmployeeForSession(session);
            JPanel form = new JPanel(new GridBagLayout());
            form.setOpaque(true);
            form.setBackground(Color.WHITE);
            form.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder("Employee Details"),
                    BorderFactory.createEmptyBorder(12, 12, 12, 12)
            ));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            String employeeId = employee != null
                    ? String.valueOf(employee.employeeId)
                    : (session.employeeId == null ? "N/A" : String.valueOf(session.employeeId));
            String employeeName = employee != null ? employee.employeeName : session.username;
            String emailId = employee != null ? employee.emailId : "N/A";
            String password = employee != null ? employee.password : "N/A";
            String phoneNumber = employee != null ? employee.phoneNumber : "N/A";
            String department = employee != null ? employee.department : "N/A";
            String role = employee != null ? employee.role : "Employee";
            String dateOfBirth = (employee != null && employee.dateOfBirth != null)
                    ? employee.dateOfBirth.toString() : "N/A";
            String gender = employee != null ? employee.gender : "N/A";
            String dateOfJoining = (employee != null && employee.dateOfJoining != null)
                    ? employee.dateOfJoining.toString() : "N/A";
            int currentYear = LocalDate.now().getYear();
            int usedCasualAndSickLeaves = calculateUsedCasualAndSickLeaveDays(
                    session.employeeId == null ? -1 : session.employeeId, currentYear);
            int remainingLeaveCount = Math.max(0, YEARLY_NON_DEDUCTED_LEAVE_DAYS - usedCasualAndSickLeaves);

            int row = 0;
            addProfileRow(form, gbc, row++, "Employee ID", employeeId);
            addProfileRow(form, gbc, row++, "Employee Name", employeeName);
            addProfileRow(form, gbc, row++, "Email ID", emailId);
            addProfileRow(form, gbc, row++, "Password", password);
            addProfileRow(form, gbc, row++, "Phone Number", phoneNumber);
            addProfileRow(form, gbc, row++, "Department", department);
            addProfileRow(form, gbc, row++, "Role", role);
            addProfileRow(form, gbc, row++, "Date of Birth", dateOfBirth);
            addProfileRow(form, gbc, row++, "Gender", gender);
            addProfileRow(form, gbc, row++, "Date of Joining", dateOfJoining);
            addProfileRow(form, gbc, row++, "Used Casual + Sick Leaves (" + currentYear + ")",
                    String.valueOf(usedCasualAndSickLeaves));
            addProfileRow(form, gbc, row, "Remaining Leave Count (" + currentYear + ")",
                    String.valueOf(remainingLeaveCount));

            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(backButton);

            JScrollPane profileScroll = new JScrollPane(form);
            profileScroll.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            profileScroll.getVerticalScrollBar().setUnitIncrement(16);
            add(profileScroll, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
        }
    }

    // -------------------- EMPLOYEE ADMIN WINDOWS --------------------
    private static class AddEmployeeWindow extends JFrame {
        private final JTextField employeeIdField = new JTextField(14);
        private final JTextField employeeNameField = new JTextField(14);
        private final JTextField emailField = new JTextField(14);
        private final JPasswordField passwordField = new JPasswordField(14);
        private final JTextField phoneField = new JTextField(14);
        private final JTextField departmentField = new JTextField(14);
        private final JComboBox<String> roleBox =
                new JComboBox<>(new String[] {"Employee", "Admin"});
        private final JTextField dobField = new JTextField(14);
        private final JComboBox<String> genderBox =
                new JComboBox<>(new String[] {"Male", "Female", "Other"});
        private final JTextField joiningDateField = new JTextField(14);
        private LocalDate selectedDob = LocalDate.now();
        private LocalDate selectedJoiningDate = LocalDate.now();

        AddEmployeeWindow() {
            setTitle("Add Employee");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            JPanel form = new JPanel(new GridBagLayout());
            makePanelTransparent(form);
            form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            addFormRow(form, gbc, row++, "Employee ID", employeeIdField);
            addFormRow(form, gbc, row++, "Employee Name", employeeNameField);
            addFormRow(form, gbc, row++, "Email ID", emailField);
            addFormRow(form, gbc, row++, "Password", passwordField);
            addFormRow(form, gbc, row++, "Phone Number", phoneField);
            addFormRow(form, gbc, row++, "Department", departmentField);
            addFormRow(form, gbc, row++, "Role", roleBox);
            addFormRow(form, gbc, row++, "Date of Birth", createDatePickerRow(dobField, true));
            addFormRow(form, gbc, row++, "Gender", genderBox);
            addFormRow(form, gbc, row, "Date of Joining", createDatePickerRow(joiningDateField, false));

            JButton addButton = new JButton("Add Employee");
            styleButton(addButton, COLOR_SUCCESS);
            addButton.addActionListener(e -> addEmployee());
            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(addButton);
            footer.add(backButton);

            add(form, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);

            dobField.setEditable(false);
            joiningDateField.setEditable(false);
            dobField.setText(selectedDob.toString());
            joiningDateField.setText(selectedJoiningDate.toString());
        }

        private void addEmployee() {
            String idText = employeeIdField.getText().trim();
            String name = employeeNameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String phone = phoneField.getText().trim();
            String department = departmentField.getText().trim();
            String role = roleBox.getSelectedItem().toString();
            String dobText = dobField.getText().trim();
            String gender = genderBox.getSelectedItem().toString();
            String joiningText = joiningDateField.getText().trim();

            if (idText.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()
                    || phone.isEmpty() || department.isEmpty()
                    || dobText.isEmpty() || joiningText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int employeeId;
            try {
                employeeId = Integer.parseInt(idText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Employee ID must be numeric.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (findEmployeeById(employeeId) != null) {
                JOptionPane.showMessageDialog(this, "Employee ID already exists.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (findEmployeeByEmail(email) != null) {
                JOptionPane.showMessageDialog(this, "Email ID already exists.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate dob;
            LocalDate doj;
            try {
                dob = LocalDate.parse(dobText);
                doj = LocalDate.parse(joiningText);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Enter dates in yyyy-MM-dd format.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            EmployeeRecord employee = new EmployeeRecord(
                    employeeId, name, email, password, phone, department,
                    role, dob, gender, doj
            );
            EMPLOYEES.add(employee);
            saveEmployees();
            JOptionPane.showMessageDialog(this, "Employee added successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            clearForm();
        }

        private void clearForm() {
            employeeIdField.setText("");
            employeeNameField.setText("");
            emailField.setText("");
            passwordField.setText("");
            phoneField.setText("");
            departmentField.setText("");
            roleBox.setSelectedIndex(0);
            selectedDob = LocalDate.now();
            dobField.setText(selectedDob.toString());
            genderBox.setSelectedIndex(0);
            selectedJoiningDate = LocalDate.now();
            joiningDateField.setText(selectedJoiningDate.toString());
        }

        private JPanel createDatePickerRow(JTextField field, boolean isDobField) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            makePanelTransparent(row);
            JButton pickButton = new JButton("Pick");
            styleButton(pickButton, COLOR_NEUTRAL);
            pickButton.addActionListener(e -> {
                LocalDate current = isDobField ? selectedDob : selectedJoiningDate;
                LocalDate selected = CalendarPickerDialog.pickDate(this, current);
                if (selected == null) {
                    return;
                }
                if (isDobField) {
                    selectedDob = selected;
                    dobField.setText(selectedDob.toString());
                } else {
                    selectedJoiningDate = selected;
                    joiningDateField.setText(selectedJoiningDate.toString());
                }
            });
            row.add(field, BorderLayout.CENTER);
            row.add(pickButton, BorderLayout.EAST);
            return row;
        }
    }

    private static class RemoveEmployeeWindow extends JFrame {
        private final JTextField employeeIdField = new JTextField(14);

        RemoveEmployeeWindow() {
            setTitle("Remove Employee");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            JPanel form = new JPanel(new GridBagLayout());
            makePanelTransparent(form);
            form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8, 8, 8, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            addFormRow(form, gbc, 0, "Employee ID", employeeIdField);

            JButton removeButton = new JButton("Remove Employee");
            styleButton(removeButton, COLOR_DANGER);
            removeButton.addActionListener(e -> removeEmployee());
            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(removeButton);
            footer.add(backButton);

            add(form, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);
        }

        private void removeEmployee() {
            String idText = employeeIdField.getText().trim();
            if (idText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter Employee ID.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int employeeId;
            try {
                employeeId = Integer.parseInt(idText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Employee ID must be numeric.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            EmployeeRecord employee = findEmployeeById(employeeId);
            if (employee == null) {
                JOptionPane.showMessageDialog(this, "Employee not found.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
            if ("Admin".equalsIgnoreCase(employee.role)) {
                JOptionPane.showMessageDialog(this, "Admin account cannot be removed.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int choice = JOptionPane.showConfirmDialog(
                    this,
                    "Remove employee " + employee.employeeName + " (ID " + employee.employeeId + ")?",
                    "Confirm Remove",
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (choice != JOptionPane.OK_OPTION) {
                return;
            }

            EMPLOYEES.remove(employee);
            saveEmployees();
            JOptionPane.showMessageDialog(this, "Employee removed successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            employeeIdField.setText("");
        }
    }

    private static class UpdateEmployeeWindow extends JFrame {
        private final JTextField lookupEmployeeIdField = new JTextField(10);
        private final JTextField employeeIdField = new JTextField(14);
        private final JTextField employeeNameField = new JTextField(14);
        private final JTextField emailField = new JTextField(14);
        private final JPasswordField passwordField = new JPasswordField(14);
        private final JTextField phoneField = new JTextField(14);
        private final JTextField departmentField = new JTextField(14);
        private final JComboBox<String> roleBox =
                new JComboBox<>(new String[] {"Employee", "Admin"});
        private final JTextField dobField = new JTextField(14);
        private final JComboBox<String> genderBox =
                new JComboBox<>(new String[] {"Male", "Female", "Other"});
        private final JTextField joiningDateField = new JTextField(14);
        private Integer loadedEmployeeId;
        private LocalDate selectedDob = LocalDate.now();
        private LocalDate selectedJoiningDate = LocalDate.now();

        UpdateEmployeeWindow() {
            setTitle("Update Employee");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            JPanel form = new JPanel(new GridBagLayout());
            makePanelTransparent(form);
            form.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 5, 5, 5);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(new JLabel("Search Employee ID"), gbc);
            gbc.gridx = 1;
            form.add(lookupEmployeeIdField, gbc);
            JButton loadButton = new JButton("Load");
            styleButton(loadButton, COLOR_PRIMARY);
            loadButton.addActionListener(e -> loadEmployee());
            gbc.gridx = 2;
            form.add(loadButton, gbc);

            int row = 1;
            addFormRow(form, gbc, row++, "Employee ID", employeeIdField);
            addFormRow(form, gbc, row++, "Employee Name", employeeNameField);
            addFormRow(form, gbc, row++, "Email ID", emailField);
            addFormRow(form, gbc, row++, "Password", passwordField);
            addFormRow(form, gbc, row++, "Phone Number", phoneField);
            addFormRow(form, gbc, row++, "Department", departmentField);
            addFormRow(form, gbc, row++, "Role", roleBox);
            addFormRow(form, gbc, row++, "Date of Birth", createDatePickerRow(dobField, true));
            addFormRow(form, gbc, row++, "Gender", genderBox);
            addFormRow(form, gbc, row, "Date of Joining", createDatePickerRow(joiningDateField, false));

            JButton updateButton = new JButton("Update Employee");
            styleButton(updateButton, COLOR_WARNING);
            updateButton.addActionListener(e -> updateEmployee());
            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(updateButton);
            footer.add(backButton);

            add(form, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);

            dobField.setEditable(false);
            joiningDateField.setEditable(false);
            clearForm();
        }

        private void loadEmployee() {
            String idText = lookupEmployeeIdField.getText().trim();
            if (idText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Enter Employee ID to load.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int employeeId;
            try {
                employeeId = Integer.parseInt(idText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Employee ID must be numeric.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            EmployeeRecord employee = findEmployeeById(employeeId);
            if (employee == null) {
                JOptionPane.showMessageDialog(this, "Employee not found.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            loadedEmployeeId = employee.employeeId;
            employeeIdField.setText(String.valueOf(employee.employeeId));
            employeeNameField.setText(employee.employeeName);
            emailField.setText(employee.emailId);
            passwordField.setText(employee.password);
            phoneField.setText(employee.phoneNumber);
            departmentField.setText(employee.department);
            roleBox.setSelectedItem(employee.role);
            selectedDob = employee.dateOfBirth != null ? employee.dateOfBirth : LocalDate.now();
            dobField.setText(selectedDob.toString());
            genderBox.setSelectedItem(employee.gender);
            selectedJoiningDate = employee.dateOfJoining != null ? employee.dateOfJoining : LocalDate.now();
            joiningDateField.setText(selectedJoiningDate.toString());
        }

        private void updateEmployee() {
            if (loadedEmployeeId == null) {
                JOptionPane.showMessageDialog(this, "Load employee first.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            String idText = employeeIdField.getText().trim();
            String name = employeeNameField.getText().trim();
            String email = emailField.getText().trim();
            String password = new String(passwordField.getPassword());
            String phone = phoneField.getText().trim();
            String department = departmentField.getText().trim();
            String role = roleBox.getSelectedItem().toString();
            String dobText = dobField.getText().trim();
            String gender = genderBox.getSelectedItem().toString();
            String joiningText = joiningDateField.getText().trim();

            if (idText.isEmpty() || name.isEmpty() || email.isEmpty() || password.isEmpty()
                    || phone.isEmpty() || department.isEmpty() || dobText.isEmpty() || joiningText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int newEmployeeId;
            try {
                newEmployeeId = Integer.parseInt(idText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Employee ID must be numeric.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int targetIndex = findEmployeeIndexById(loadedEmployeeId);
            if (targetIndex < 0) {
                JOptionPane.showMessageDialog(this, "Loaded employee not found.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            EmployeeRecord existingById = findEmployeeById(newEmployeeId);
            if (existingById != null && existingById.employeeId != loadedEmployeeId) {
                JOptionPane.showMessageDialog(this, "Employee ID already exists.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            EmployeeRecord existingByEmail = findEmployeeByEmail(email);
            if (existingByEmail != null && existingByEmail.employeeId != loadedEmployeeId) {
                JOptionPane.showMessageDialog(this, "Email ID already exists.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            LocalDate dob;
            LocalDate doj;
            try {
                dob = LocalDate.parse(dobText);
                doj = LocalDate.parse(joiningText);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this,
                        "Enter dates in yyyy-MM-dd format.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            EmployeeRecord updated = new EmployeeRecord(
                    newEmployeeId, name, email, password, phone, department,
                    role, dob, gender, doj
            );
            EMPLOYEES.set(targetIndex, updated);
            saveEmployees();
            loadedEmployeeId = updated.employeeId;
            lookupEmployeeIdField.setText(String.valueOf(updated.employeeId));
            JOptionPane.showMessageDialog(this, "Employee updated successfully.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
        }

        private void clearForm() {
            employeeIdField.setText("");
            employeeNameField.setText("");
            emailField.setText("");
            passwordField.setText("");
            phoneField.setText("");
            departmentField.setText("");
            roleBox.setSelectedIndex(0);
            selectedDob = LocalDate.now();
            dobField.setText(selectedDob.toString());
            genderBox.setSelectedIndex(0);
            selectedJoiningDate = LocalDate.now();
            joiningDateField.setText(selectedJoiningDate.toString());
        }

        private JPanel createDatePickerRow(JTextField field, boolean isDobField) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            makePanelTransparent(row);
            JButton pickButton = new JButton("Pick");
            styleButton(pickButton, COLOR_NEUTRAL);
            pickButton.addActionListener(e -> {
                LocalDate current = isDobField ? selectedDob : selectedJoiningDate;
                LocalDate selected = CalendarPickerDialog.pickDate(this, current);
                if (selected == null) {
                    return;
                }
                if (isDobField) {
                    selectedDob = selected;
                    dobField.setText(selectedDob.toString());
                } else {
                    selectedJoiningDate = selected;
                    joiningDateField.setText(selectedJoiningDate.toString());
                }
            });
            row.add(field, BorderLayout.CENTER);
            row.add(pickButton, BorderLayout.EAST);
            return row;
        }
    }

    private static class ViewEmployeesWindow extends JFrame {
        private final JTable table = new JTable();

        ViewEmployeesWindow() {
            setTitle("All Employees");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            table.setModel(buildEmployeesTableModel());
            JScrollPane scrollPane = new JScrollPane(table);
            add(scrollPane, BorderLayout.CENTER);

            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JButton logoutButton = new JButton("Logout");
            styleButton(logoutButton, COLOR_PRIMARY);
            logoutButton.addActionListener(e -> logoutAndShowLogin());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(backButton);
            footer.add(logoutButton);
            add(footer, BorderLayout.SOUTH);
        }
    }

    // -------------------- APPLY LEAVE WINDOW --------------------
    private static class ApplyLeaveWindow extends JFrame {
        private final JTextField employeeIdField = new JTextField(12);
        private final JTextField employeeNameField = new JTextField(12);
        private final JComboBox<String> leaveTypeBox =
                new JComboBox<>(new String[] {"Casual Leave", "Sick Leave", "Earned Leave", "Paid Leave"});
        private final JTextField durationField = new JTextField(12);
        private final JTextField fromDateField = new JTextField(10);
        private final JTextField toDateField = new JTextField(10);
        private LocalDate fromDate = LocalDate.now();
        private LocalDate toDate = LocalDate.now();
        private final UserSession session;

        ApplyLeaveWindow(UserSession session) {
            this.session = session;
            setTitle("Apply Leave");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            JPanel form = new JPanel(new GridBagLayout());
            makePanelTransparent(form);
            form.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0;
            gbc.gridy = 0;
            form.add(new JLabel("Employee ID"), gbc);
            gbc.gridx = 1;
            form.add(employeeIdField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 1;
            form.add(new JLabel("Employee Name"), gbc);
            gbc.gridx = 1;
            form.add(employeeNameField, gbc);

            gbc.gridx = 0;
            gbc.gridy = 2;
            form.add(new JLabel("Leave Type"), gbc);
            gbc.gridx = 1;
            form.add(leaveTypeBox, gbc);

            gbc.gridx = 0;
            gbc.gridy = 3;
            form.add(new JLabel("From Date"), gbc);
            gbc.gridx = 1;
            form.add(createDatePickerRow(fromDateField, true), gbc);

            gbc.gridx = 0;
            gbc.gridy = 4;
            form.add(new JLabel("To Date"), gbc);
            gbc.gridx = 1;
            form.add(createDatePickerRow(toDateField, false), gbc);

            gbc.gridx = 0;
            gbc.gridy = 5;
            form.add(new JLabel("Duration (days)"), gbc);
            gbc.gridx = 1;
            form.add(durationField, gbc);

            JButton submit = new JButton("Submit");
            styleButton(submit, COLOR_SUCCESS);
            submit.addActionListener(e -> submitLeave());
            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JButton logoutButton = new JButton("Logout");
            styleButton(logoutButton, COLOR_PRIMARY);
            logoutButton.addActionListener(e -> logoutAndShowLogin());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(submit);
            footer.add(backButton);
            footer.add(logoutButton);

            add(form, BorderLayout.CENTER);
            add(footer, BorderLayout.SOUTH);

            fromDateField.setEditable(false);
            toDateField.setEditable(false);
            fromDateField.setText(fromDate.toString());
            toDateField.setText(toDate.toString());

            if (!session.isAdmin && session.employeeId != null) {
                employeeIdField.setText(String.valueOf(session.employeeId));
                employeeIdField.setEditable(false);
                employeeNameField.setText(session.username);
                employeeNameField.setEditable(false);
            }
        }

        private void submitLeave() {
            String idText = employeeIdField.getText().trim();
            String name = employeeNameField.getText().trim();
            String durationText = durationField.getText().trim();

            if (idText.isEmpty() || name.isEmpty() || durationText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Fill all fields.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int employeeId;
            int durationDays;
            try {
                employeeId = Integer.parseInt(idText);
                durationDays = Integer.parseInt(durationText);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "ID and duration must be numbers.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (!session.isAdmin && session.employeeId != null && employeeId != session.employeeId) {
                JOptionPane.showMessageDialog(this,
                        "You can only apply leave for Employee ID " + session.employeeId + ".",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (toDate.isBefore(fromDate)) {
                JOptionPane.showMessageDialog(this, "To date must be on/after from date.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (hasOverlappingLeave(employeeId, fromDate, toDate)) {
                JOptionPane.showMessageDialog(this,
                        "Leave already applied for the selected date range.",
                        "Already Applied", JOptionPane.WARNING_MESSAGE);
                return;
            }

            String leaveType = leaveTypeBox.getSelectedItem().toString();
            int requestedLeaveDays = countDeductiblePaidLeaveDays(fromDate, toDate);
            if (requestedLeaveDays > CONTINUOUS_LEAVE_LIMIT_DAYS
                    && !"Paid Leave".equals(leaveType)) {
                leaveType = "Paid Leave";
                leaveTypeBox.setSelectedItem("Paid Leave");
                JOptionPane.showMessageDialog(this,
                        "Continuous leave above " + CONTINUOUS_LEAVE_LIMIT_DAYS
                                + " days is treated as Paid Leave.",
                        "Leave Type Updated",
                        JOptionPane.INFORMATION_MESSAGE);
            }
            int year = fromDate.getYear();
            int usedLeaveDaysThisYear = countEmployeeLeaveDaysInYear(employeeId, year);
            int remainingNonDeductedDays = Math.max(0, YEARLY_NON_DEDUCTED_LEAVE_DAYS - usedLeaveDaysThisYear);
            int yearlyExcessDays = Math.max(0, requestedLeaveDays - remainingNonDeductedDays);
            int deductionDays = yearlyExcessDays;
            if (requestedLeaveDays > CONTINUOUS_LEAVE_LIMIT_DAYS) {
                deductionDays = requestedLeaveDays;
            }

            if (deductionDays > 0) {
                int deductionAmount = deductionDays * SALARY_DEDUCTION_PER_DAY;
                int choice = JOptionPane.showConfirmDialog(
                        this,
                        "Leave policy summary:\n"
                                + "Requested days (excluding Sundays): " + requestedLeaveDays + "\n"
                                + "Non-deducted balance for " + year + ": " + remainingNonDeductedDays + "\n"
                                + "Deduction days: " + deductionDays + "\n"
                                + "Amount deducted from salary: " + deductionAmount
                                + " (" + SALARY_DEDUCTION_PER_DAY + " per day).\nClick OK to apply leave.",
                        "Salary Deduction Confirmation",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);
                if (choice != JOptionPane.OK_OPTION) {
                    return;
                }
            }

            LeaveRecord record = new LeaveRecord(
                    employeeId,
                    name,
                    leaveType,
                    durationDays,
                    fromDate,
                    toDate,
                    session.username,
                    LocalDate.now(),
                    "Pending");
            RECORDS.add(record);
            saveRecords();

            JOptionPane.showMessageDialog(this, "Leave request submitted.", "Success",
                    JOptionPane.INFORMATION_MESSAGE);
            durationField.setText("");
            fromDate = LocalDate.now();
            toDate = LocalDate.now();
            fromDateField.setText(fromDate.toString());
            toDateField.setText(toDate.toString());
            leaveTypeBox.setSelectedIndex(0);
            if (!session.isAdmin && session.employeeId != null) {
                employeeIdField.setText(String.valueOf(session.employeeId));
                employeeNameField.setText(session.username);
            } else {
                employeeIdField.setText("");
                employeeNameField.setText("");
            }
        }

        private boolean hasOverlappingLeave(int employeeId, LocalDate requestedFrom, LocalDate requestedTo) {
            for (LeaveRecord existing : RECORDS) {
                if (existing.employeeId != employeeId) {
                    continue;
                }
                if ("Rejected".equalsIgnoreCase(existing.status)) {
                    continue;
                }
                boolean overlaps = !requestedTo.isBefore(existing.fromDate)
                        && !requestedFrom.isAfter(existing.toDate);
                if (overlaps) {
                    return true;
                }
            }
            return false;
        }

        private int countDeductiblePaidLeaveDays(LocalDate startDate, LocalDate endDate) {
            int days = 0;
            LocalDate current = startDate;
            while (!current.isAfter(endDate)) {
                if (current.getDayOfWeek().getValue() != 7) {
                    days++;
                }
                current = current.plusDays(1);
            }
            return days;
        }

        private int countEmployeeLeaveDaysInYear(int employeeId, int year) {
            int totalDays = 0;
            LocalDate yearStart = LocalDate.of(year, 1, 1);
            LocalDate yearEnd = LocalDate.of(year, 12, 31);
            for (LeaveRecord existing : RECORDS) {
                if (existing.employeeId != employeeId) {
                    continue;
                }
                if ("Rejected".equalsIgnoreCase(existing.status)) {
                    continue;
                }
                if (existing.toDate.isBefore(yearStart) || existing.fromDate.isAfter(yearEnd)) {
                    continue;
                }
                LocalDate effectiveStart = existing.fromDate.isBefore(yearStart) ? yearStart : existing.fromDate;
                LocalDate effectiveEnd = existing.toDate.isAfter(yearEnd) ? yearEnd : existing.toDate;
                totalDays += countDeductiblePaidLeaveDays(effectiveStart, effectiveEnd);
            }
            return totalDays;
        }

        private JPanel createDatePickerRow(JTextField field, boolean isFromDateField) {
            JPanel row = new JPanel(new BorderLayout(6, 0));
            makePanelTransparent(row);
            JButton pickButton = new JButton("Pick");
            styleButton(pickButton, COLOR_NEUTRAL);
            pickButton.addActionListener(e -> {
                LocalDate current = isFromDateField ? fromDate : toDate;
                LocalDate selected = CalendarPickerDialog.pickDate(this, current);
                if (selected != null) {
                    if (isFromDateField) {
                        fromDate = selected;
                        fromDateField.setText(fromDate.toString());
                    } else {
                        toDate = selected;
                        toDateField.setText(toDate.toString());
                    }
                }
            });
            
            row.add(field, BorderLayout.CENTER);
            row.add(pickButton, BorderLayout.EAST);
            return row;
        }
    }

    private static class CalendarPickerDialog extends JDialog {
        private static final DateTimeFormatter MONTH_FORMAT =
                DateTimeFormatter.ofPattern("MMMM yyyy");
        private static final String[] DAY_HEADERS = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        private static final String[] MONTHS = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        private final JLabel monthLabel = new JLabel("", JLabel.CENTER);
        private final JPanel dayButtonsPanel = new JPanel(new GridLayout(0, 7, 4, 4));
        private final JComboBox<String> monthBox = new JComboBox<>(MONTHS);
        private final JComboBox<Integer> yearBox = new JComboBox<>();
        private YearMonth shownMonth;
        private LocalDate pickedDate;
        private final LocalDate initialDate;
        private boolean updatingSelectors;

        private CalendarPickerDialog(JFrame owner, LocalDate initialDate) {
            super(owner, "Select Date", true);
            this.initialDate = initialDate;
            this.shownMonth = YearMonth.from(initialDate);

            setLayout(new BorderLayout(8, 8));
            ((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            int startYear = initialDate.getYear() - 80;
            int endYear = initialDate.getYear() + 20;
            for (int year = startYear; year <= endYear; year++) {
                yearBox.addItem(year);
            }

            JButton prev = new JButton("<");
            styleButton(prev, COLOR_NEUTRAL);
            prev.addActionListener(e -> {
                shownMonth = shownMonth.minusMonths(1);
                refreshCalendar();
            });

            JButton next = new JButton(">");
            styleButton(next, COLOR_NEUTRAL);
            next.addActionListener(e -> {
                shownMonth = shownMonth.plusMonths(1);
                refreshCalendar();
            });

            monthBox.addActionListener(e -> {
                if (updatingSelectors) {
                    return;
                }
                int month = monthBox.getSelectedIndex() + 1;
                shownMonth = YearMonth.of(shownMonth.getYear(), month);
                refreshCalendar();
            });
            yearBox.addActionListener(e -> {
                if (updatingSelectors || yearBox.getSelectedItem() == null) {
                    return;
                }
                int year = (int) yearBox.getSelectedItem();
                shownMonth = YearMonth.of(year, shownMonth.getMonthValue());
                refreshCalendar();
            });

            JPanel top = new JPanel(new BorderLayout());
            JPanel selectors = new JPanel(new FlowLayout(FlowLayout.CENTER, 6, 0));
            selectors.add(monthBox);
            selectors.add(yearBox);
            top.add(prev, BorderLayout.WEST);
            top.add(selectors, BorderLayout.CENTER);
            top.add(next, BorderLayout.EAST);
            top.add(monthLabel, BorderLayout.SOUTH);
            add(top, BorderLayout.NORTH);

            JPanel center = new JPanel(new BorderLayout(0, 6));
            JPanel headers = new JPanel(new GridLayout(1, 7, 4, 0));
            for (String day : DAY_HEADERS) {
                JLabel header = new JLabel(day, JLabel.CENTER);
                headers.add(header);
            }
            center.add(headers, BorderLayout.NORTH);
            center.add(dayButtonsPanel, BorderLayout.CENTER);
            add(center, BorderLayout.CENTER);

            JButton cancel = new JButton("Cancel");
            styleButton(cancel, COLOR_NEUTRAL);
            cancel.addActionListener(e -> dispose());
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            footer.add(cancel);
            add(footer, BorderLayout.SOUTH);

            refreshCalendar();
            pack();
            setResizable(false);
            setLocationRelativeTo(owner);
        }

        private void refreshCalendar() {
            updatingSelectors = true;
            monthBox.setSelectedIndex(shownMonth.getMonthValue() - 1);
            yearBox.setSelectedItem(shownMonth.getYear());
            updatingSelectors = false;

            dayButtonsPanel.removeAll();
            monthLabel.setText(shownMonth.format(MONTH_FORMAT));

            LocalDate firstDay = shownMonth.atDay(1);
            int leadingEmptyCells = firstDay.getDayOfWeek().getValue() - 1;
            for (int i = 0; i < leadingEmptyCells; i++) {
                dayButtonsPanel.add(new JLabel(""));
            }

            for (int day = 1; day <= shownMonth.lengthOfMonth(); day++) {
                LocalDate date = shownMonth.atDay(day);
                JButton dayButton = new JButton(String.valueOf(day));
                dayButton.setBackground(new Color(248, 250, 252));
                dayButton.setForeground(new Color(45, 55, 72));
                dayButton.setFocusPainted(false);
                if (date.equals(initialDate)) {
                    dayButton.setFont(dayButton.getFont().deriveFont(Font.BOLD));
                }
                dayButton.addActionListener(e -> {
                    pickedDate = date;
                    dispose();
                });
                dayButtonsPanel.add(dayButton);
            }

            int totalCells = leadingEmptyCells + shownMonth.lengthOfMonth();
            int trailingCells = (7 - (totalCells % 7)) % 7;
            for (int i = 0; i < trailingCells; i++) {
                dayButtonsPanel.add(new JLabel(""));
            }

            dayButtonsPanel.revalidate();
            dayButtonsPanel.repaint();
        }

        static LocalDate pickDate(JFrame owner, LocalDate initialDate) {
            CalendarPickerDialog dialog = new CalendarPickerDialog(owner, initialDate);
            dialog.setVisible(true);
            return dialog.pickedDate;
        }
    }

    // -------------------- APPROVE LEAVE WINDOW --------------------
    private static class ApproveLeaveWindow extends JFrame {
        private final UserSession session;
        private final JTable table = new JTable();

        ApproveLeaveWindow(UserSession session) {
            this.session = session;
            setTitle("Approve / Reject Leave");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            if (!session.isAdmin) {
                JOptionPane.showMessageDialog(this, "Only admin/manager can approve leave.",
                        "Access Denied", JOptionPane.WARNING_MESSAGE);
            }

            refreshTable();
            JScrollPane scroll = new JScrollPane(table);
            makeScrollPaneTransparent(scroll);
            add(scroll, BorderLayout.CENTER);

            JButton approve = new JButton("Approve");
            styleButton(approve, COLOR_SUCCESS);
            approve.addActionListener(e -> updateStatus("Approved"));
            JButton reject = new JButton("Reject");
            styleButton(reject, COLOR_DANGER);
            reject.addActionListener(e -> updateStatus("Rejected"));
            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JButton logoutButton = new JButton("Logout");
            styleButton(logoutButton, COLOR_PRIMARY);
            logoutButton.addActionListener(e -> logoutAndShowLogin());

            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(approve);
            footer.add(reject);
            footer.add(backButton);
            footer.add(logoutButton);
            add(footer, BorderLayout.SOUTH);
        }

        private void updateStatus(String newStatus) {
            if (!session.isAdmin) {
                JOptionPane.showMessageDialog(this, "Access denied.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int row = table.getSelectedRow();
            if (row < 0) {
                JOptionPane.showMessageDialog(this, "Select a pending record.", "Error",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            int recordIndex = getPendingRecordIndexByRow(row);
            if (recordIndex < 0 || recordIndex >= RECORDS.size()) {
                JOptionPane.showMessageDialog(this, "Selected record is invalid. Refresh and retry.",
                        "Error", JOptionPane.ERROR_MESSAGE);
                refreshTable();
                return;
            }
            LeaveRecord record = RECORDS.get(recordIndex);
            record.status = newStatus;
            saveRecords();
            refreshTable();
        }

        private void refreshTable() {
            table.setModel(buildPendingTableModel());
            table.setPreferredScrollableViewportSize(new Dimension(740, 280));
        }

        private int getPendingRecordIndexByRow(int row) {
            int pendingRow = 0;
            for (int i = 0; i < RECORDS.size(); i++) {
                if (!"Pending".equals(RECORDS.get(i).status)) {
                    continue;
                }
                if (pendingRow == row) {
                    return i;
                }
                pendingRow++;
            }
            return -1;
        }
    }

    // -------------------- SEARCH LEAVE WINDOW --------------------
    private static class SearchLeaveWindow extends JFrame {
        private final JTextField employeeIdField = new JTextField(10);
        private final JComboBox<String> statusBox =
                new JComboBox<>(new String[] {"All", "Pending", "Approved", "Rejected"});
        private final JTable table = new JTable();

        SearchLeaveWindow() {
            setTitle("Search Leave Records");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);

            JPanel filters = new JPanel();
            makePanelTransparent(filters);
            filters.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            filters.add(new JLabel("Employee ID"));
            filters.add(employeeIdField);
            filters.add(new JLabel("Status"));
            filters.add(statusBox);
            JButton searchButton = new JButton("Search");
            styleButton(searchButton, COLOR_PRIMARY);
            searchButton.addActionListener(e -> refreshTable());
            filters.add(searchButton);
            add(filters, BorderLayout.NORTH);

            JScrollPane tableScroll = new JScrollPane(table);
            makeScrollPaneTransparent(tableScroll);
            add(tableScroll, BorderLayout.CENTER);
            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JButton logoutButton = new JButton("Logout");
            styleButton(logoutButton, COLOR_PRIMARY);
            logoutButton.addActionListener(e -> logoutAndShowLogin());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(backButton);
            footer.add(logoutButton);
            add(footer, BorderLayout.SOUTH);
            refreshTable();
        }

        private void refreshTable() {
            String idText = employeeIdField.getText().trim();
            String status = statusBox.getSelectedItem().toString();

            List<LeaveRecord> filtered = new ArrayList<>();
            for (int i = 0; i < RECORDS.size(); i++) {
                LeaveRecord record = RECORDS.get(i);
                boolean matchesId = true;
                if (!idText.isEmpty()) {
                    try {
                        int id = Integer.parseInt(idText);
                        matchesId = record.employeeId == id;
                    } catch (NumberFormatException ex) {
                        matchesId = false;
                    }
                }
                boolean matchesStatus = "All".equals(status) || record.status.equals(status);
                if (matchesId && matchesStatus) {
                    filtered.add(record);
                }
            }

            table.setModel(buildTableModel(filtered, true));
        }
    }

    // -------------------- VIEW RECORDS WINDOW --------------------
    private static class ViewLeaveRecordsWindow extends JFrame {
        private final JTable table = new JTable();
        private final UserSession session;

        ViewLeaveRecordsWindow(UserSession session) {
            this.session = session;
            setTitle("All Leave Records");
            setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            setSize(760, 500);
            setLocationRelativeTo(null);
            applyPlainWhiteBackground(this);
            JScrollPane tableScroll = new JScrollPane(table);
            makeScrollPaneTransparent(tableScroll);
            add(tableScroll, BorderLayout.CENTER);
            JButton backButton = new JButton("Back");
            styleButton(backButton, COLOR_NEUTRAL);
            backButton.addActionListener(e -> dispose());
            JButton logoutButton = new JButton("Logout");
            styleButton(logoutButton, COLOR_PRIMARY);
            logoutButton.addActionListener(e -> logoutAndShowLogin());
            JPanel footer = new JPanel();
            makePanelTransparent(footer);
            footer.add(backButton);
            footer.add(logoutButton);
            add(footer, BorderLayout.SOUTH);
            refreshTable();
        }

        private void refreshTable() {
            List<LeaveRecord> visibleRecords = new ArrayList<>();
            for (LeaveRecord record : RECORDS) {
                if (session.isAdmin) {
                    visibleRecords.add(record);
                    continue;
                }
                boolean isRequester = session.username.equalsIgnoreCase(record.requestedBy);
                boolean matchesEmployeeId = session.employeeId == null || session.employeeId == record.employeeId;
                if (isRequester && matchesEmployeeId) {
                    visibleRecords.add(record);
                }
            }
            table.setModel(buildTableModel(visibleRecords, true));
        }
    }

    // -------------------- USER SESSION --------------------
    private static class UserSession {
        final String username;
        final boolean isAdmin;
        final Integer employeeId;

        UserSession(String username, boolean isAdmin, Integer employeeId) {
            this.username = username;
            this.isAdmin = isAdmin;
            this.employeeId = employeeId;
        }

        static UserSession authenticate(String username, String password) {
            String loginId = username == null ? "" : username.trim();
            for (EmployeeRecord employee : EMPLOYEES) {
                boolean matchesEmail = employee.emailId != null
                        && employee.emailId.equalsIgnoreCase(loginId);
                boolean matchesName = employee.employeeName != null
                        && employee.employeeName.equalsIgnoreCase(loginId);
                boolean matchesId = String.valueOf(employee.employeeId).equals(loginId);
                boolean usernameMatches = matchesEmail || matchesName || matchesId;

                if (!usernameMatches || !employee.password.equals(password)) {
                    continue;
                }
                boolean isAdmin = "Admin".equalsIgnoreCase(employee.role);
                Integer employeeId = isAdmin ? null : employee.employeeId;
                return new UserSession(employee.employeeName, isAdmin, employeeId);
            }
            return null;
        }
    }

    // -------------------- TABLE HELPERS --------------------
    private static void addFormRow(JPanel form, GridBagConstraints gbc, int row, String labelText, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(labelText), gbc);
        gbc.gridx = 1;
        form.add(field, gbc);
    }

    private static void addProfileRow(JPanel form, GridBagConstraints gbc, int row, String labelText, String value) {
        gbc.gridx = 0;
        gbc.gridy = row;
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        form.add(label, gbc);
        gbc.gridx = 1;
        String safeValue = (value == null || value.trim().isEmpty()) ? "N/A" : value;
        JTextField valueField = new JTextField(safeValue);
        valueField.setEditable(false);
        valueField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        valueField.setPreferredSize(new Dimension(300, 30));
        valueField.setBackground(Color.WHITE);
        form.add(valueField, gbc);
    }

    private static EmployeeRecord findEmployeeById(int employeeId) {
        for (EmployeeRecord employee : EMPLOYEES) {
            if (employee.employeeId == employeeId) {
                return employee;
            }
        }
        return null;
    }

    private static int findEmployeeIndexById(int employeeId) {
        for (int i = 0; i < EMPLOYEES.size(); i++) {
            if (EMPLOYEES.get(i).employeeId == employeeId) {
                return i;
            }
        }
        return -1;
    }

    private static EmployeeRecord findEmployeeByEmail(String emailId) {
        for (EmployeeRecord employee : EMPLOYEES) {
            if (employee.emailId.equalsIgnoreCase(emailId)) {
                return employee;
            }
        }
        return null;
    }

    private static EmployeeRecord findEmployeeByName(String employeeName) {
        for (EmployeeRecord employee : EMPLOYEES) {
            if (employee.employeeName.equalsIgnoreCase(employeeName)) {
                return employee;
            }
        }
        return null;
    }

    private static EmployeeRecord findEmployeeForSession(UserSession session) {
        if (session.employeeId != null) {
            EmployeeRecord byId = findEmployeeById(session.employeeId);
            if (byId != null) {
                return byId;
            }
        }
        EmployeeRecord byName = findEmployeeByName(session.username);
        if (byName != null) {
            return byName;
        }
        return findEmployeeByEmail(session.username);
    }

    private static DefaultTableModel buildTableModel(List<LeaveRecord> records, boolean includeIndex) {
        String[] cols = includeIndex
                ? new String[] {"S.No", "Employee ID", "Employee Name", "Leave Type", "Duration",
                "From Date", "To Date", "Requested By", "Requested On", "Status"}
                : new String[] {"Employee ID", "Employee Name", "Leave Type", "Duration",
                "From Date", "To Date", "Requested By", "Requested On", "Status"};

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (int i = 0; i < records.size(); i++) {
            LeaveRecord record = records.get(i);
            if (includeIndex) {
                model.addRow(new Object[] {
                        i + 1,
                        record.employeeId,
                        record.employeeName,
                        record.leaveType,
                        record.durationDays,
                        record.fromDate,
                        record.toDate,
                        record.requestedBy,
                        record.requestedOn,
                        record.status
                });
            } else {
                model.addRow(new Object[] {
                        record.employeeId,
                        record.employeeName,
                        record.leaveType,
                        record.durationDays,
                        record.fromDate,
                        record.toDate,
                        record.requestedBy,
                        record.requestedOn,
                        record.status
                });
            }
        }
        return model;
    }

    private static DefaultTableModel buildEmployeesTableModel() {
        String[] cols = {"S.No", "Employee ID", "Employee Name", "Email ID", "Password",
                "Phone Number", "Department", "Role", "Date of Birth", "Gender", "Date of Joining"};

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        List<EmployeeRecord> employeesOnly = new ArrayList<>();
        for (EmployeeRecord employee : EMPLOYEES) {
            if ("Admin".equalsIgnoreCase(employee.role)) {
                continue;
            }
            employeesOnly.add(employee);
        }
        employeesOnly.sort(Comparator.comparingInt(e -> e.employeeId));

        int serialNo = 1;
        for (EmployeeRecord employee : employeesOnly) {
            model.addRow(new Object[] {
                    serialNo++,
                    employee.employeeId,
                    employee.employeeName,
                    employee.emailId,
                    employee.password,
                    employee.phoneNumber,
                    employee.department,
                    employee.role,
                    employee.dateOfBirth,
                    employee.gender,
                    employee.dateOfJoining
            });
        }
        return model;
    }

    private static int calculateUsedCasualAndSickLeaveDays(int employeeId, int year) {
        if (employeeId < 0) {
            return 0;
        }
        int usedDays = 0;
        LocalDate yearStart = LocalDate.of(year, 1, 1);
        LocalDate yearEnd = LocalDate.of(year, 12, 31);

        for (LeaveRecord record : RECORDS) {
            if (record.employeeId != employeeId) {
                continue;
            }
            if ("Rejected".equalsIgnoreCase(record.status)) {
                continue;
            }
            boolean casualOrSick = "Casual Leave".equalsIgnoreCase(record.leaveType)
                    || "Sick Leave".equalsIgnoreCase(record.leaveType);
            if (!casualOrSick) {
                continue;
            }
            if (record.toDate.isBefore(yearStart) || record.fromDate.isAfter(yearEnd)) {
                continue;
            }

            LocalDate effectiveStart = record.fromDate.isBefore(yearStart) ? yearStart : record.fromDate;
            LocalDate effectiveEnd = record.toDate.isAfter(yearEnd) ? yearEnd : record.toDate;
            usedDays += countDaysExcludingSundays(effectiveStart, effectiveEnd);
        }
        return usedDays;
    }

    private static int countDaysExcludingSundays(LocalDate startDate, LocalDate endDate) {
        int days = 0;
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            if (current.getDayOfWeek().getValue() != 7) {
                days++;
            }
            current = current.plusDays(1);
        }
        return days;
    }

    private static void printEmployeesToConsole() {
        List<EmployeeRecord> employeesOnly = new ArrayList<>();
        for (EmployeeRecord employee : EMPLOYEES) {
            if (!"Admin".equalsIgnoreCase(employee.role)) {
                employeesOnly.add(employee);
            }
        }
        employeesOnly.sort(Comparator.comparingInt(e -> e.employeeId));

        System.out.printf("%-6s %-10s %-18s %-25s %-14s %-14s %-10s %-12s %-8s %-12s%n",
                "S.No", "Emp ID", "Name", "Email", "Phone", "Department",
                "Role", "DOB", "Gender", "DOJ");
        System.out.println("---------------------------------------------------------------------------------------------------------------");

        int serialNo = 1;
        for (EmployeeRecord employee : employeesOnly) {
            System.out.printf("%-6d %-10d %-18s %-25s %-14s %-14s %-10s %-12s %-8s %-12s%n",
                    serialNo++,
                    employee.employeeId,
                    employee.employeeName,
                    employee.emailId,
                    employee.phoneNumber,
                    employee.department,
                    employee.role,
                    employee.dateOfBirth,
                    employee.gender,
                    employee.dateOfJoining);
        }

        if (employeesOnly.isEmpty()) {
            System.out.println("No employees found.");
        }
    }

    private static void syncEmployeesToMySql() {
        String createTableSql = "CREATE TABLE IF NOT EXISTS employees ("
                + "employee_id INT PRIMARY KEY,"
                + "employee_name VARCHAR(100) NOT NULL,"
                + "email_id VARCHAR(150) NOT NULL UNIQUE,"
                + "password VARCHAR(100) NOT NULL,"
                + "phone_number VARCHAR(20),"
                + "department VARCHAR(80),"
                + "role VARCHAR(20),"
                + "date_of_birth DATE,"
                + "gender VARCHAR(20),"
                + "date_of_joining DATE"
                + ")";

        String upsertSql = "INSERT INTO employees "
                + "(employee_id, employee_name, email_id, password, phone_number, department, role, "
                + "date_of_birth, gender, date_of_joining) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) "
                + "ON DUPLICATE KEY UPDATE "
                + "employee_name=VALUES(employee_name), "
                + "email_id=VALUES(email_id), "
                + "password=VALUES(password), "
                + "phone_number=VALUES(phone_number), "
                + "department=VALUES(department), "
                + "role=VALUES(role), "
                + "date_of_birth=VALUES(date_of_birth), "
                + "gender=VALUES(gender), "
                + "date_of_joining=VALUES(date_of_joining)";

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            System.out.println("MySQL JDBC driver not found. Add mysql-connector-j to classpath.");
            return;
        }

        try (Connection con = DriverManager.getConnection(MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD);
             Statement st = con.createStatement();
             PreparedStatement ps = con.prepareStatement(upsertSql)) {

            st.executeUpdate(createTableSql);

            for (EmployeeRecord employee : EMPLOYEES) {
                ps.setInt(1, employee.employeeId);
                ps.setString(2, employee.employeeName);
                ps.setString(3, employee.emailId);
                ps.setString(4, employee.password);
                ps.setString(5, employee.phoneNumber);
                ps.setString(6, employee.department);
                ps.setString(7, employee.role);
                ps.setDate(8, java.sql.Date.valueOf(employee.dateOfBirth));
                ps.setString(9, employee.gender);
                ps.setDate(10, java.sql.Date.valueOf(employee.dateOfJoining));
                ps.executeUpdate();
            }

            System.out.println("Employees synced to MySQL table: employees");
            System.out.println("Now run in MySQL terminal: SELECT * FROM employees;");
        } catch (SQLException ex) {
            System.out.println("MySQL sync failed: " + ex.getMessage());
        }
    }

    private static List<LeaveRecord> filterByStatus(String status) {
        List<LeaveRecord> filtered = new ArrayList<>();
        for (LeaveRecord record : RECORDS) {
            if (record.status.equals(status)) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private static DefaultTableModel buildPendingTableModel() {
        String[] cols = {"Index", "Employee ID", "Employee Name", "Leave Type", "Duration",
                "From Date", "To Date", "Requested By", "Requested On", "Status"};

        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        int serialNo = 1;
        for (int i = 0; i < RECORDS.size(); i++) {
            LeaveRecord record = RECORDS.get(i);
            if (!"Pending".equals(record.status)) {
                continue;
            }
            model.addRow(new Object[] {
                    serialNo++,
                    record.employeeId,
                    record.employeeName,
                    record.leaveType,
                    record.durationDays,
                    record.fromDate,
                    record.toDate,
                    record.requestedBy,
                    record.requestedOn,
                    record.status
            });








            
        }
        return model;
    }

    private static void initializeDefaultEmployees() {
        EMPLOYEES.clear();
        EMPLOYEES.add(new EmployeeRecord(
                1, "admin", "admin", "admin123", "9000000000", "Management",
                "Admin", LocalDate.of(1990, 1, 1), "Other", LocalDate.of(2020, 1, 1)
        ));
        EMPLOYEES.add(new EmployeeRecord(
                101, "emp1", "emp1", "emp1@123", "9000000001", "Engineering",
                "Employee", LocalDate.of(1998, 1, 1), "Male", LocalDate.of(2023, 1, 1)
        ));
        EMPLOYEES.add(new EmployeeRecord(
                102, "emp2", "emp2", "emp2@123", "9000000002", "AIML",
                "Employee", LocalDate.of(1999, 2, 1), "Female", LocalDate.of(2023, 2, 1)
        ));
        EMPLOYEES.add(new EmployeeRecord(
                103, "emp3", "emp3", "emp3@123", "9000000003", "Operations",
                "Employee", LocalDate.of(2000, 3, 1), "Other", LocalDate.of(2023, 3, 1)
        ));
    }

    private static void saveEmployees() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(EMPLOYEES_FILE))) {
            out.writeObject(new ArrayList<>(EMPLOYEES));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadEmployees() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(EMPLOYEES_FILE))) {
            Object data = in.readObject();
            if (data instanceof List<?>) {
                EMPLOYEES.clear();
                EMPLOYEES.addAll((List<EmployeeRecord>) data);
            }
        } catch (IOException | ClassNotFoundException ex) {
            initializeDefaultEmployees();
            saveEmployees();
        }
        if (EMPLOYEES.isEmpty()) {
            initializeDefaultEmployees();
            saveEmployees();
        }
    }

    private static void saveRecords() {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(RECORDS_FILE))) {
            out.writeObject(new ArrayList<>(RECORDS));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @SuppressWarnings("unchecked")
    private static void loadRecords() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(RECORDS_FILE))) {
            Object data = in.readObject();
            if (data instanceof List<?>) {
                RECORDS.clear();
                RECORDS.addAll((List<LeaveRecord>) data);
            }
        } catch (IOException | ClassNotFoundException ex) {
            // First run or unreadable file: start with empty records.
        }
    }
}
