package tn.esprit.tic.civiAgora.controller.saas;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.esprit.tic.civiAgora.dto.saasDto.SaasPlatformSettingsDto;
import tn.esprit.tic.civiAgora.service.SaasPlatformSettingsService;

@RestController
@RequestMapping("/saas/settings")
@RequiredArgsConstructor
public class SaasPlatformSettingsController {

    private final SaasPlatformSettingsService settingsService;

    @GetMapping
    public ResponseEntity<SaasPlatformSettingsDto> getSettings() {
        return ResponseEntity.ok(settingsService.getSettings());
    }

    @PutMapping
    public ResponseEntity<SaasPlatformSettingsDto> updateSettings(@RequestBody SaasPlatformSettingsDto dto) {
        return ResponseEntity.ok(settingsService.updateSettings(dto));
    }
}
