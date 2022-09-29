package com.aws.rest;

import com.aws.entities.WorkItem;
import com.aws.services.SendMessages;
import jxl.write.WriteException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@ComponentScan(basePackages = {"com.aws.services"})
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("api/")
public class MainController {
    private final RetrieveItems ri;
    private final WriteExcel writeExcel;

    private final SendMessages sm;

    private final InjectWorkService iw;

    @Autowired
    MainController(
        RetrieveItems ri,
        WriteExcel writeExcel,
        SendMessages sm,
        InjectWorkService iw
    ) {
        this.ri = ri;
        this.writeExcel = writeExcel;
        this.sm = sm;
        this.iw = iw;
    }

    @GetMapping("items/{state}")
    public List<WorkItem> getItems(@PathVariable String state) {
        if (state.compareTo("active") == 0) {
            return ri.getItemsDataSQLReport("0");
        } else {
            return ri.getItemsDataSQLReport("1");
        }
    }

    @PutMapping("mod/{id}")
    public String modUser(@PathVariable String id) {
        ri.flipItemArchive(id);
        return id + " was archived";
    }

    @PostMapping("add")
    public String addItems(@RequestBody Map<String, String> payload) {
         String name = App.username;
         String guide = payload.get("guide");
         String description = payload.get("description");
         String status = payload.get("status");

         WorkItem item = new WorkItem();
         item.setGuide(guide);
         item.setDescription(description);
         item.setName(name);
         item.setStatus(status);

         String id = iw.injectNewSubmission(item);
         return "Added " + id;
    }

    @PutMapping("report/{email}")
    public String sendReport(@PathVariable String email){
        List<WorkItem> list = ri.getItemsDataSQLReport("0");
        try {
            InputStream is = writeExcel.write(list);
            sm.sendReport(is, email);
            return "Report generated & sent" ;
        } catch (IOException | WriteException e) {
            e.printStackTrace();
        }
        return "Failed to generate report";
    }
}
