package pe.utec.dbp.labreserve.auth;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.utec.dbp.labreserve.auth.dto.LoginRequestDTO;
import pe.utec.dbp.labreserve.auth.dto.LoginResponseDTO;
import pe.utec.dbp.labreserve.auth.dto.RegisterRequestDTO;
import pe.utec.dbp.labreserve.auth.dto.RegisterResponseDTO;
import pe.utec.dbp.labreserve.model.Role;
import pe.utec.dbp.labreserve.model.UserAccount;
import pe.utec.dbp.labreserve.security.JwtTokenProvider;
import pe.utec.dbp.labreserve.shared.ConflictException;
import pe.utec.dbp.labreserve.shared.UnauthorizedException;

@Service
public class AuthService {

    /** Todo registro publico entra como estudiante; los otros roles se asignan internamente. */
    private static final Role DEFAULT_ROLE = Role.ROLE_STUDENT;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public RegisterResponseDTO register(RegisterRequestDTO request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new ConflictException("El username ya esta registrado");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("El email ya esta registrado");
        }

        UserAccount account = new UserAccount(
                request.username(),
                request.email(),
                passwordEncoder.encode(request.password()),
                DEFAULT_ROLE
        );

        return RegisterResponseDTO.from(userRepository.save(account));
    }

    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        } catch (AuthenticationException ex) {
            throw new UnauthorizedException("Usuario o password incorrectos");
        }

        UserAccount account = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new UnauthorizedException("Usuario o password incorrectos"));

        return new LoginResponseDTO(tokenProvider.issue(account), tokenProvider.getExpiresIn());
    }
}
