package com.example.mjg.spring.filtering;

import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataPage;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.data.SimpleDataPage;
import com.example.mjg.spring.exceptions.InvalidRepositoryMethodException;
import com.example.mjg.spring.repositories.MigratableSpringRepository;
import com.example.mjg.spring.stores.SpringRepositoryStore;
import com.example.mjg.utils.ReflectionUtils;
import com.example.mjg.utils.functional.interfaces.Function2;
import com.example.mjg.utils.functional.interfaces.Function3;
import com.example.mjg.utils.functional.interfaces.Function4;
import com.example.mjg.utils.functional.interfaces.Function5;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@AllArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(callSuper = false)
public class SpringRepositoryFilterSet<
    T extends MigratableEntity,
    ID extends Serializable
> implements DataFilterSet {
    private final Method repositoryMethod;

    private final List<Object> callbackArgs;

    public DataPage<T, ID, SpringRepositoryFilterSet<T, ID>>
    executeAndReturnDataPage(SpringRepositoryStore<T, ID> store, Pageable pageable)
    throws Exception {
        Page<T> page = executeAndReturnPage(store.getRepository(), pageable);

        return new SimpleDataPage<>(
            store,
            this,
            pageable.getPageNumber(),
            page.getContent()
        );
    }

    public Page<T> executeAndReturnPage(MigratableSpringRepository<T, ID> repository, Pageable pageable)
    throws Exception {
        repositoryMethod.setAccessible(true);
        Object returnValue;
        List<Object> args = new ArrayList<>(callbackArgs);
        args.add(pageable);
        try {
            returnValue = repositoryMethod.invoke(repository, args.toArray());
        } catch (InvocationTargetException | IllegalAccessException e) {
            throw new InvalidRepositoryMethodException(repositoryMethod, e);
        }

        if (returnValue instanceof Page) {
            @SuppressWarnings("unchecked")
            Page<T> realPage = (Page<T>) returnValue;
            return realPage;
        }

        throw new InvalidRepositoryMethodException(repositoryMethod, "method not returning an instance of Page<T>");
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>
    >
    SpringRepositoryFilterSet<T, ID> findAll() {
        return SpringRepositoryFilterSet.of(REPO::findAll);
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>
    >
    SpringRepositoryFilterSet<T, ID> findAllByIdIn(Set<ID> ids) {
        return SpringRepositoryFilterSet.of(REPO::findAllByIdInOrderByIdAsc, ids);
    }


    ////////////////////////////////////////////////////////
    /// OVERLOADS FOR SEVERAL FUNCTION PARAMETER ARITIES ///
    ////////////////////////////////////////////////////////

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>
    >
    SpringRepositoryFilterSet<T, ID> of(Function2<REPO, Pageable, Page<T>> lambda) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of()
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A
    >
    SpringRepositoryFilterSet<T, ID> of(Function3<REPO, A, Pageable, Page<T>> lambda, A a) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B
    >
    SpringRepositoryFilterSet<T, ID> of(Function4<REPO, A, B, Pageable, Page<T>> lambda, A a, B b) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C
    >
    SpringRepositoryFilterSet<T, ID> of(Function5<REPO, A, B, C, Pageable, Page<T>> lambda, A a, B b, C c) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c)
        );
    }

    // TODO: Function 6 through 16
}
