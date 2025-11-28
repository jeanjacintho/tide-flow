package br.jeanjacintho.tideflow.user_service.script;

import br.jeanjacintho.tideflow.user_service.model.*;
import br.jeanjacintho.tideflow.user_service.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    @Transactional
    public void run(String... args) {
        if (args.length > 0 && "seed".equals(args[0])) {
            System.out.println("🌱 Iniciando população do banco de dados...");
            
            seedDatabase();
            
            System.out.println("✅ Banco de dados populado com sucesso!");
        }
    }

    private void seedDatabase() {
        Company moredevs = createCompany();
        List<Department> departments = createDepartments(moredevs);
        User rootUser = createRootUser();
        List<User> employees = createEmployees(moredevs, departments);
        
        System.out.println("✅ Empresa 'moredevs' criada com ID: " + moredevs.getId());
        System.out.println("✅ Usuário root criado: " + rootUser.getUsername());
        System.out.println("✅ " + employees.size() + " funcionários criados");
        System.out.println("✅ " + departments.size() + " departamentos criados");
    }

    private Company createCompany() {
        Optional<Company> existing = companyRepository.findAll().stream()
            .filter(c -> "moredevs".equalsIgnoreCase(c.getName()))
            .findFirst();
        if (existing.isPresent()) {
            System.out.println("⚠️  Empresa 'moredevs' já existe. Pulando criação...");
            return existing.get();
        }

        Company company = new Company();
        company.setName("moredevs");
        company.setDomain("moredevs.com");
        company.setSubscriptionPlan(SubscriptionPlan.ENTERPRISE);
        company.setMaxEmployees(100);
        company.setStatus(CompanyStatus.ACTIVE);
        company.setBillingEmail("billing@moredevs.com");
        company.setBillingAddress("Rua das Empresas, 123 - São Paulo, SP");
        company.setTaxId("12.345.678/0001-90");
        
        return companyRepository.save(company);
    }

    private List<Department> createDepartments(Company company) {
        List<Department> departments = new ArrayList<>();
        
        String[][] deptData = {
            {"Desenvolvimento", "Equipe de desenvolvimento de software"},
            {"Design", "Design e UX/UI"},
            {"Marketing", "Marketing digital e comunicação"},
            {"Vendas", "Vendas e relacionamento com clientes"},
            {"Recursos Humanos", "Gestão de pessoas"},
            {"Financeiro", "Contabilidade e finanças"}
        };

        for (String[] data : deptData) {
            final String deptName = data[0];
            final String deptDescription = data[1];
            
            boolean exists = departmentRepository.existsByCompanyIdAndName(company.getId(), deptName);
            if (exists) {
                Department existing = departmentRepository.findByCompanyId(company.getId()).stream()
                    .filter(d -> deptName.equals(d.getName()))
                    .findFirst()
                    .orElse(null);
                if (existing != null) {
                    departments.add(existing);
                    continue;
                }
            }

            Department dept = new Department();
            dept.setCompany(company);
            dept.setName(deptName);
            dept.setDescription(deptDescription);
            
            departments.add(departmentRepository.save(dept));
        }

        return departments;
    }

    private User createRootUser() {
        Optional<User> existing = userRepository.findByUsername("root");
        if (existing.isPresent()) {
            System.out.println("⚠️  Usuário root já existe. Pulando criação...");
            return existing.get();
        }

        User root = new User();
        root.setName("Administrador do Sistema");
        root.setUsername("root");
        root.setEmail("root@tideflow.com");
        root.setPassword(passwordEncoder.encode("root123"));
        root.setSystemRole(SystemRole.SYSTEM_ADMIN);
        root.setIsActive(true);
        root.setMustChangePassword(false);
        root.setPrivacyConsentStatus(PrivacyConsentStatus.ACCEPTED);
        root.setPrivacyNoticeAcknowledged(true);
        root.setDataSharingEnabled(true);
        
        Company firstCompany = companyRepository.findAll().stream().findFirst().orElse(null);
        if (firstCompany != null) {
            List<Department> depts = departmentRepository.findByCompanyId(firstCompany.getId());
            if (!depts.isEmpty()) {
                root.setCompany(firstCompany);
                root.setDepartment(depts.get(0));
            }
        }

        return userRepository.save(root);
    }

    private List<User> createEmployees(Company company, List<Department> departments) {
        List<User> employees = new ArrayList<>();
        
        String[][] employeeData = {
            {"João Silva", "joao.silva@moredevs.com", "Desenvolvimento", "DEV001"},
            {"Maria Santos", "maria.santos@moredevs.com", "Desenvolvimento", "DEV002"},
            {"Pedro Oliveira", "pedro.oliveira@moredevs.com", "Desenvolvimento", "DEV003"},
            {"Ana Costa", "ana.costa@moredevs.com", "Design", "DSG001"},
            {"Carlos Pereira", "carlos.pereira@moredevs.com", "Design", "DSG002"},
            {"Julia Ferreira", "julia.ferreira@moredevs.com", "Marketing", "MKT001"},
            {"Roberto Alves", "roberto.alves@moredevs.com", "Marketing", "MKT002"},
            {"Fernanda Lima", "fernanda.lima@moredevs.com", "Vendas", "VND001"},
            {"Lucas Souza", "lucas.souza@moredevs.com", "Vendas", "VND002"},
            {"Patricia Rocha", "patricia.rocha@moredevs.com", "Recursos Humanos", "RH001"},
            {"Marcos Dias", "marcos.dias@moredevs.com", "Financeiro", "FIN001"},
            {"Beatriz Martins", "beatriz.martins@moredevs.com", "Financeiro", "FIN002"},
            {"Rafael Gomes", "rafael.gomes@moredevs.com", "Desenvolvimento", "DEV004"},
            {"Camila Ribeiro", "camila.ribeiro@moredevs.com", "Design", "DSG003"},
            {"Thiago Barbosa", "thiago.barbosa@moredevs.com", "Marketing", "MKT003"}
        };

        String[] cities = {"São Paulo", "Rio de Janeiro", "Belo Horizonte", "Curitiba", "Porto Alegre"};
        String[] states = {"SP", "RJ", "MG", "PR", "RS"};

        Random random = new Random();

        for (String[] data : employeeData) {
            String name = data[0];
            String email = data[1];
            String deptName = data[2];
            String employeeId = data[3];

            Optional<User> existing = userRepository.findByEmail(email);
            if (existing.isPresent()) {
                employees.add(existing.get());
                continue;
            }

            Department dept = departments.stream()
                .filter(d -> d.getName().equals(deptName))
                .findFirst()
                .orElse(departments.get(0));

            User employee = new User();
            employee.setName(name);
            employee.setEmail(email);
            employee.setUsername(email.split("@")[0]);
            employee.setPassword(passwordEncoder.encode("senha123"));
            employee.setSystemRole(SystemRole.NORMAL);
            employee.setCompany(company);
            employee.setDepartment(dept);
            employee.setEmployeeId(employeeId);
            employee.setIsActive(true);
            employee.setMustChangePassword(false);
            employee.setPrivacyConsentStatus(PrivacyConsentStatus.ACCEPTED);
            employee.setPrivacyNoticeAcknowledged(true);
            employee.setDataSharingEnabled(true);
            employee.setCity(cities[random.nextInt(cities.length)]);
            employee.setState(states[random.nextInt(states.length)]);
            employee.setPhone("+55 11 9" + String.format("%08d", random.nextInt(100000000)));

            employees.add(userRepository.save(employee));
        }

        return employees;
    }
}
