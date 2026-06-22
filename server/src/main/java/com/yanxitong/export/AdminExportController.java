package com.yanxitong.export;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/exports/banquets/{banquetId}")
public class AdminExportController {
    private final ExportService exportService;

    public AdminExportController(ExportService exportService) {
        this.exportService = exportService;
    }

    @GetMapping("/gifts.csv")
    public ResponseEntity<ByteArrayResource> gifts(@PathVariable Long banquetId) {
        return download(exportService.exportGifts(banquetId), new MediaType("text", "csv"));
    }

    @GetMapping("/gifts.xlsx")
    public ResponseEntity<ByteArrayResource> giftsXlsx(@PathVariable Long banquetId) {
        return download(exportService.exportGiftsXlsx(banquetId), MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/rsvp.csv")
    public ResponseEntity<ByteArrayResource> rsvp(@PathVariable Long banquetId) {
        return download(exportService.exportRsvp(banquetId), new MediaType("text", "csv"));
    }

    @GetMapping("/rsvp.xlsx")
    public ResponseEntity<ByteArrayResource> rsvpXlsx(@PathVariable Long banquetId) {
        return download(exportService.exportRsvpXlsx(banquetId), MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @GetMapping("/favor.csv")
    public ResponseEntity<ByteArrayResource> favor(@PathVariable Long banquetId) {
        return download(exportService.exportFavor(banquetId), new MediaType("text", "csv"));
    }

    @GetMapping("/favor.xlsx")
    public ResponseEntity<ByteArrayResource> favorXlsx(@PathVariable Long banquetId) {
        return download(exportService.exportFavorXlsx(banquetId), MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    private ResponseEntity<ByteArrayResource> download(ExportFile file, MediaType contentType) {
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename())
                        .build()
                        .toString())
                .contentLength(file.content().length)
                .body(new ByteArrayResource(file.content()));
    }
}
