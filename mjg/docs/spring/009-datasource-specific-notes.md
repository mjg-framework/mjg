# Datasource-specific Notes

- [Datasource-specific Notes](#datasource-specific-notes)
  - [Introduction](#introduction)
  - [Datasources that do not support all-or-nothing transaction](#datasources-that-do-not-support-all-or-nothing-transaction)
    - [Problem](#problem)
    - [Solution](#solution)
    - [In the case of Spring Data JPA](#in-the-case-of-spring-data-jpa)
    - [In the case of Spring Data MongoDB](#in-the-case-of-spring-data-mongodb)
  - [See also](#see-also)

## Introduction

This document highlights information that
are crucial when getting `mjg` to work with
certain datasources, due to their limitations.

## Datasources that do not support all-or-nothing transaction

### Problem

[In the introductory article](./000-overview.md#what-is-mjg)
we noted that special treatment is required for datasources that
do not support all-or-nothing transaction.

For such a datasource, `mjg` can't reliably retry
failed records - one of the most crucial fault
tolerance strategies of the framework.

Imagine `mjg` needs to save several output records
(returned by `transform()`) into the output datastore,
and the datastore fails to save one of them.

Now, since the persistence is not atomically
transactional, the datastore might have successfully
saved *some* of those records.

But `mjg`, assuming the datasource did not save
any of them thanks to transactionality, would
declare all those output records as failed,
and retry all of them as needed, *including those*
*that are already saved to the store*. This could
result in serious data duplication.

### Solution

It is recommended to find out ways to atomically
save multiple records as a whole. Many kind of
datasources support it.

If you absolutely can't find a way for that:

1. Set `batchSize = 1` in any `@TransformAndSaveTo`
    annotation that means to save records to
    such a fragile datasource.

2. In the related `transform()` methods, only
    return at most 1 output record.

In so doing, `mjg` would save at most 1 output record
in one batch to the output store, corresponding
to 1 input record only, so that once there is a
failure, `mjg` could be sure:

1. That input record is the only one that caused the
    problem;
2. That output record has NOT been saved to the output
    datastore;
3. There is no other output record (linked to that
    same input record) has been saved to the output
    datastore before this failed output record
    (since there is just one output
    record and it failed).
    
    That's why you should only return at most 
    1 output record in the related `transform()`
    methods.

With these guarantees, `mjg` could safely retry it later,
or report the error in the *migration progress*
with the true cause of the issue, including the
exception raised and that problematic
input record.

### In the case of Spring Data JPA

In Spring Data JPA, the `saveAll()` method of
`JpaRepository` (and its parent `CrudRepository`)
is transactional by default. So for JPA/SQL
repositories/datastores, you don't need to worry
about this problem - `mjg-adapter-spring` will
handle it safely.

### In the case of Spring Data MongoDB

While SQL databases support true atomic transaction
just fine, MongoDB does not, by default.

It could fail to save a document midway
when doing `saveAll()`, and it is difficult
to tell which records/documents were
successfully saved and which weren't.

There are two solutions for this.

1. Set up a replica set (MongoDB 4.0+)
    or sharded cluster (MongoDB 4.2+)
    for your MongoDB instance.
    
    Update the MongoDB connection string
    accordingly (e.g. in `application.yaml`
    for Spring Boot applications).

    That way, your MongoDB connection
    will support "multi-document transactions",
    and `mjg-adapter-spring-data-mongodb`
    automatically enables transactional
    batch save.

    There are tutorials on the web
    for doing that, provided that
    you have the right to alter
    MongoDB configurations.

2. If you can't do so, you have to fall
    to the last resort as described
    above, though that
    would make the overall migration
    process significantly slower.

## See also

- [Home](./README.md)
- [Previous: Migration Progress JSON Format](./008-migration-progress-json-format.md)
- [Next: Review](./010-review.md)
