package com.aws.rest;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.rdsdata.RdsDataClient;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementRequest;
import software.amazon.awssdk.services.rdsdata.model.ExecuteStatementResponse;
import software.amazon.awssdk.services.rdsdata.model.Field;
import software.amazon.awssdk.services.rdsdata.model.SqlParameter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component()
public class WorkItemRepository implements CrudRepository<WorkItem, String> {
    static final String active = "ACT";
    static final String archived = "ARCH";
    static final String database = "auroraappdb";
    static final String secretArn = "arn:aws:secretsmanager:us-east-1:659765859849:secret:docexampleauroraappsecret8B-ZRuYk32DvFmC-Dz2N2y";
    static final String resourceArn = "arn:aws:rds:us-east-1:659765859849:cluster:docexampleauroraapp-docexampleauroraappclustereb7e-vqwzivzly59p";

    static RdsDataClient getClient() {
        return RdsDataClient.builder().region(App.region).build();
    }

    static ExecuteStatementResponse execute(String sqlStatement, List<SqlParameter> parameters) {
        var sqlRequest = ExecuteStatementRequest.builder()
                .resourceArn(resourceArn)
                .secretArn(secretArn)
                .database(database)
                .sql(sqlStatement)
                .parameters(parameters)
                .build();
        return getClient().executeStatement(sqlRequest);
    }

    static SqlParameter param(String name, String value) {
        return SqlParameter.builder().name(name).value(Field.builder().stringValue(value).build()).build();
    }

    @Override
    public <S extends WorkItem> S save(S item) {
        String name = item.getName();
        String guide = item.getGuide();
        String description = item.getDescription();
        String status = item.getStatus();

        String workId = UUID.randomUUID().toString();
        String date = LocalDateTime.now().toString();

        String sql = "INSERT INTO Work (idwork, username, date, description, guide, status) VALUES" +
                "(:idwork, :username, :date, :description, :guide, :status);";
        List<SqlParameter> paremeters = List.of(
                param("idwork", workId),
                param("username", name),
                param("date", date),
                param("description", description),
                param("guide", guide),
                param("status", status)
        );

        execute(sql, paremeters).records();
        return (S) findById(workId).get();
    }

    @Override
    public <S extends WorkItem> Iterable<S> saveAll(Iterable<S> entities) {
        return StreamSupport.stream(entities.spliterator(), true).map(this::save)::iterator;
    }

    @Override
    public Optional<WorkItem> findById(String s) {
        String sqlStatement = "SELECT idwork, date, description, guide, status, username FROM Work WHERE idwork = :id;";
        List<SqlParameter> parameters = List.of(param("id", s));
        var result = execute(sqlStatement, parameters)
                .records()
                .stream()
                .map(WorkItem::from)
                .collect(Collectors.toUnmodifiableList());
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(result.get(0));
        }
    }

    @Override
    public boolean existsById(String s) {
        return findById(s).isPresent();
    }

    @Override
    public Iterable<WorkItem> findAll() {
        return findAllWithStatus(active);
    }

    public Iterable<WorkItem> findAllWithStatus(String status) {
        String sqlStatement = "SELECT idwork, date, description, guide, status, username " +
                "FROM Work WHERE status = :arch ;";
        List<SqlParameter> parameters = List.of(
                param("arch", status)
        );
        return execute(sqlStatement, parameters)
                .records()
                .stream()
                .map(WorkItem::from)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public Iterable<WorkItem> findAllById(Iterable<String> strings) {
        var item = findById(strings.iterator().next());
        if (item.isPresent()) {
            return List.of(item.get());
        }
        return List.of();
    }

    @Override
    public long count() {
        String sqlStatement = "SELECT COUNT(idwork) AS count FROM Work;";
        List<SqlParameter> parameters = List.of();
        return execute(sqlStatement, parameters)
                .records()
                .stream()
                .map(fields -> fields.get(0).longValue()).iterator().next();
    }

    @Override
    public void deleteById(String s) {
        String sqlStatement = "DELETE FROM Work WHERE idwork = :id;";
        List<SqlParameter> parameters = List.of(param("id", s));
        execute(sqlStatement, parameters);
    }

    @Override
    public void delete(WorkItem entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends String> strings) {
        strings.forEach(this::deleteById);
    }

    @Override
    public void deleteAll(Iterable<? extends WorkItem> entities) {
        deleteAllById(StreamSupport.stream(entities.spliterator(), false).map(WorkItem::getId)::iterator);
    }

    @Override
    public void deleteAll() {
        String sqlStatement = "DELETE FROM Work;";
        List<SqlParameter> parameters = List.of();
        execute(sqlStatement, parameters);
    }
}
