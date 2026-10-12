package ar.edu.utn.frre.m3.adapter.web;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
@Controller
public class UiPreviewController {
    @GetMapping({"/ui/preview","/ui/preview/**"})
    public String live(HttpServletRequest request) {
        return "redirect:"+request.getRequestURI().replaceFirst("/ui/preview","/ui");
    }
}
