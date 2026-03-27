package com.dionialves.AsteraComm.config;

import com.dionialves.AsteraComm.asterisk.aors.AorRepository;
import com.dionialves.AsteraComm.asterisk.aors.Aors;
import com.dionialves.AsteraComm.asterisk.auth.Auth;
import com.dionialves.AsteraComm.asterisk.auth.AuthRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.Endpoint;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointRepository;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointStatus;
import com.dionialves.AsteraComm.asterisk.endpoint.EndpointStatusRepository;
import com.dionialves.AsteraComm.cdr.CdrRecord;
import com.dionialves.AsteraComm.cdr.CdrRepository;
import com.dionialves.AsteraComm.circuit.Circuit;
import com.dionialves.AsteraComm.circuit.CircuitRepository;
import com.dionialves.AsteraComm.customer.Customer;
import com.dionialves.AsteraComm.customer.CustomerRepository;
import com.dionialves.AsteraComm.did.DID;
import com.dionialves.AsteraComm.did.DIDRepository;
import com.dionialves.AsteraComm.plan.PackageType;
import com.dionialves.AsteraComm.plan.Plan;
import com.dionialves.AsteraComm.plan.PlanRepository;
import com.dionialves.AsteraComm.trunk.Trunk;
import com.dionialves.AsteraComm.trunk.TrunkRepository;
import com.dionialves.AsteraComm.user.User;
import com.dionialves.AsteraComm.user.UserRepository;
import com.dionialves.AsteraComm.user.UserRole;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Random;

/**
 * Popula o banco de dados com dados de teste.
 * Executado apenas quando o profile "dev" está ativo.
 */
@RequiredArgsConstructor
@Component
@Profile("dev")
public class DevDataSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataSeeder.class);

    private static final int TOTAL_CIRCUITOS = 10;
    private static final double PERCENTUAL_ONLINE = 0.80;
    private static final long CODIGO_INICIAL = 100000L;
    private static final long DID_INICIAL = 4933333333L;
    private static final long DID_INCREMENTO = 1111L;

    private static final String[] NOMES_CLIENTES = {
        "Acme Telecom Ltda", "Beta Comunicações", "Conecta Fácil ME",
        "Delta Phones", "Estrela Net", "Fone Rápido SA",
        "Globo Connect", "Horizonte Telecom", "Inova VoIP",
        "Jovem Telecom"
    };

    private final CdrRepository cdrRepository;
    private final CircuitRepository circuitRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final DIDRepository didRepository;
    private final TrunkRepository trunkRepository;
    private final AuthRepository authRepository;
    private final AorRepository aorRepository;
    private final EndpointRepository endpointRepository;
    private final EndpointStatusRepository endpointStatusRepository;
    private final UserRepository userRepository;
    private final Random random = new Random();

    @Override
    public void run(String... args) {
        if (circuitRepository.count() > 0) {
            log.info("Dados já existem no banco. Pulando seed de desenvolvimento.");
            return;
        }

        log.info("Iniciando seed de dados de desenvolvimento...");

        criarUsuarioAdmin();

        Trunk trunk = criarTronco();
        Plan plan = criarPlanoDev();

        log.info("Criando {} circuitos vinculados ao tronco '{}'...", TOTAL_CIRCUITOS, trunk.getName());

        int onlineCount = 0;
        int offlineCount = 0;

        for (int i = 0; i < TOTAL_CIRCUITOS; i++) {
            String number = String.valueOf(CODIGO_INICIAL + i);
            String password = gerarSenhaAleatoria();
            boolean isOnline = random.nextDouble() < PERCENTUAL_ONLINE;

            Customer customer = criarCliente(NOMES_CLIENTES[i]);

            Circuit circuit = new Circuit();
            circuit.setNumber(number);
            circuit.setPassword(password);
            circuit.setTrunkName(trunk.getName());
            circuit.setCustomer(customer);
            circuit.setPlan(plan);
            circuitRepository.save(circuit);

            String didNumber = String.valueOf(DID_INICIAL + (long) i * DID_INCREMENTO);
            criarDid(didNumber, circuit);

            Auth auth = criarAuth(number, password);
            authRepository.save(auth);

            Aors aor = criarAors(number);
            aorRepository.save(aor);

            Endpoint endpoint = criarEndpoint(number, auth, aor, trunk.getName());
            endpointRepository.save(endpoint);

            EndpointStatus status = criarEndpointStatus(endpoint, isOnline);
            endpointStatusRepository.save(status);

            if (isOnline) onlineCount++;
            else offlineCount++;
        }

        log.info("Seed finalizado! Circuitos: {} | Online: {} | Offline: {}",
                TOTAL_CIRCUITOS, onlineCount, offlineCount);

        criarCdrs(trunk.getName());
    }

    private void criarCdrs(String trunkName) {
        if (cdrRepository.count() > 0) return;

        String[] dispositions = {"ANSWERED", "ANSWERED", "ANSWERED", "ANSWERED", "ANSWERED",
                                 "ANSWERED", "ANSWERED", "NO ANSWER", "NO ANSWER", "BUSY"};
        String[] dstPrefixes = {"11", "21", "31", "41", "51", "61", "71", "81", "91"};

        for (int i = 0; i < 50; i++) {
            String src = String.valueOf(CODIGO_INICIAL + random.nextInt(TOTAL_CIRCUITOS));
            String dstArea = dstPrefixes[random.nextInt(dstPrefixes.length)];
            String dst = dstArea + String.format("%08d", random.nextInt(100000000));
            String disposition = dispositions[random.nextInt(dispositions.length)];

            int duration = disposition.equals("ANSWERED")
                    ? 30 + random.nextInt(570)
                    : 5 + random.nextInt(25);
            int billsec = disposition.equals("ANSWERED") ? duration : 0;

            LocalDateTime calldate = LocalDateTime.now()
                    .minusDays(random.nextInt(90))
                    .minusHours(random.nextInt(24))
                    .minusMinutes(random.nextInt(60));

            CdrRecord cdr = new CdrRecord();
            cdr.setUniqueId(String.valueOf(System.nanoTime() + i));
            cdr.setCalldate(calldate);
            cdr.setClid("\"" + src + "\" <" + src + ">");
            cdr.setSrc(src);
            cdr.setDst(dst);
            cdr.setDcontext("internal-" + trunkName);
            cdr.setChannel("PJSIP/" + src + "-" + String.format("%08x", random.nextInt()));
            cdr.setDstchannel("PJSIP/" + trunkName + "-" + String.format("%08x", random.nextInt()));
            cdr.setLastapp("Dial");
            cdr.setLastdata(dst);
            cdr.setDuration(duration);
            cdr.setBillsec(billsec);
            cdr.setDisposition(disposition);
            cdr.setAmaflags(3);

            cdrRepository.save(cdr);
        }

        log.info("50 registros CDR criados.");
    }

    private Customer criarCliente(String nome) {
        return customerRepository.findAll().stream()
                .filter(c -> c.getName().equals(nome))
                .findFirst()
                .orElseGet(() -> {
                    Customer customer = new Customer();
                    customer.setName(nome);
                    customer.setEnabled(true);
                    customer.setCreatedAt(LocalDateTime.now());
                    customer.setUpdatedAt(LocalDateTime.now());
                    return customerRepository.save(customer);
                });
    }

    private Plan criarPlanoDev() {
        return planRepository.findAll().stream()
                .filter(p -> p.getName().equals("Plano Dev"))
                .findFirst()
                .orElseGet(() -> {
                    Plan plan = new Plan();
                    plan.setName("Plano Dev");
                    plan.setMonthlyPrice(new BigDecimal("99.90"));
                    plan.setFixedLocal(new BigDecimal("0.0600"));
                    plan.setFixedLongDistance(new BigDecimal("0.1200"));
                    plan.setMobileLocal(new BigDecimal("0.3500"));
                    plan.setMobileLongDistance(new BigDecimal("0.4500"));
                    plan.setPackageType(PackageType.UNIFIED);
                    plan.setPackageTotalMinutes(100);
                    Plan saved = planRepository.save(plan);
                    log.info("Plano de desenvolvimento criado: '{}'", saved.getName());
                    return saved;
                });
    }

    private void criarDid(String number, Circuit circuit) {
        if (didRepository.existsByNumber(number)) return;
        DID did = new DID();
        did.setNumber(number);
        did.setCircuit(circuit);
        didRepository.save(did);
    }

    private Trunk criarTronco() {
        if (trunkRepository.existsByName("dev-operadora")) {
            return trunkRepository.findByName("dev-operadora").orElseThrow();
        }

        Trunk trunk = new Trunk();
        trunk.setName("dev-operadora");
        trunk.setHost("sip.dev-operadora.local");
        trunk.setUsername("dev-user");
        trunk.setPassword("dev-senha");
        trunk.setPrefix(null);
        trunkRepository.save(trunk);

        log.info("Tronco fictício criado: '{}'", trunk.getName());
        return trunk;
    }

    private String gerarSenhaAleatoria() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder senha = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            senha.append(chars.charAt(random.nextInt(chars.length())));
        }
        return senha.toString();
    }

    private String gerarIpAleatorio() {
        if (random.nextBoolean()) {
            return String.format("192.168.%d.%d", random.nextInt(256), random.nextInt(256));
        } else {
            return String.format("10.0.%d.%d", random.nextInt(256), random.nextInt(256));
        }
    }

    private String gerarRttAleatorio() {
        int rtt = 5 + random.nextInt(146);
        return rtt + "ms";
    }

    private Auth criarAuth(String number, String password) {
        Auth auth = new Auth();
        auth.setId(number);
        auth.setAuthType("userpass");
        auth.setUsername(number);
        auth.setPassword(password);
        return auth;
    }

    private Aors criarAors(String number) {
        Aors aor = new Aors();
        aor.setId(number);
        aor.setMaxContacts("1");
        aor.setRemoveExisting("yes");
        aor.setQualifyFrequency("60");
        aor.setDefaultExpiration("3600");
        return aor;
    }

    private Endpoint criarEndpoint(String number, Auth auth, Aors aors, String trunkName) {
        Endpoint endpoint = new Endpoint();
        endpoint.setId(number);
        endpoint.setAors(aors);
        endpoint.setAuth(auth);
        endpoint.setContext("internal-" + trunkName);
        endpoint.setDisallow("all");
        endpoint.setAllow("ulaw,alaw");
        endpoint.setDirect_media("no");
        endpoint.setForce_rport("yes");
        endpoint.setRewriteContact("yes");
        endpoint.setRtpSymmetric("yes");
        endpoint.setCallerid(number);
        return endpoint;
    }

    private void criarUsuarioAdmin() {
        if (userRepository.existsByUsername("admin@asteracomm.com")) {
            log.info("Usuário admin já existe.");
            return;
        }

        User admin = new User(
                "Administrador",
                "admin@asteracomm.com",
                "$2a$12$H3aKgAf.Q0eCN7UupcUAS.M5t/cGTmRb8fgznmtag8tFzfLed5tKe",
                UserRole.ADMIN);

        userRepository.save(admin);
        log.info("Usuário admin criado: admin@asteracomm.com");
    }

    private EndpointStatus criarEndpointStatus(Endpoint endpoint, boolean isOnline) {
        EndpointStatus status = new EndpointStatus();
        status.setEndpoint(endpoint);
        status.setOnline(isOnline);
        status.setCheckedAt(LocalDateTime.now().minusMinutes(random.nextInt(60)));

        if (isOnline) {
            status.setIp(gerarIpAleatorio());
            status.setRtt(gerarRttAleatorio());
        } else {
            status.setIp(null);
            status.setRtt(null);
        }

        return status;
    }
}
