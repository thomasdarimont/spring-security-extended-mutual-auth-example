package demo.api;

import demo.security.CompoundX509AuthenticationToken;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
class GreetingController {

    @GetMapping("/hello")
    Object greet(Principal principal) {

        CompoundX509AuthenticationToken token = (CompoundX509AuthenticationToken) principal;

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("greeting", String.format("Hello %s", principal.getName()));
        map.put("currentTime", Instant.now());
        map.put("username", token.getName());
        map.put("roles", token.getAuthorities());
        map.put("clientName", ((User)token.getPreAuthenticatedPrincipal()).getUsername());

        return map;
    }
}

