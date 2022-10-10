package com.aws.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@ComponentScan(basePackages = {"com.aws.services"})
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/items")
public class MainController {
    private final WorkItemRepository repository;

    @Autowired
    MainController(
        WorkItemRepository repository
    ) {
        this.repository = repository;
    }

    @GetMapping("{id}")
    public WorkItem getItem(@PathVariable String id) {
        var item = repository.findById(id);
        if (item.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
        return item.get();
    }
    @GetMapping("")
    public List<WorkItem> getItems() {
        var result = repository.findAll();
        return StreamSupport.stream(result.spliterator(), false)
                .collect(Collectors.toUnmodifiableList());
    }

    @PutMapping("{id}:archive")
    public WorkItem modUser(@PathVariable String id) {
        var item = repository.findById(id);
        if (item.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Entity not found");
        var workItem = item.get();
        workItem.setStatus("0");
        return repository.save(workItem);
    }

    @PostMapping(value = "", consumes = {"application/json"})
    public WorkItem addItem(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String guide = payload.get("guide");
        String description = payload.get("description");

        WorkItem item = new WorkItem();
        String workId = UUID.randomUUID().toString();
        String date = LocalDateTime.now().toString();
        item.setId(workId);
        item.setGuide(guide);
        item.setDescription(description);
        item.setName(name);
        item.setDate(date);
        item.setStatus(WorkItemRepository.active);

        return repository.save(item);
    }
}
