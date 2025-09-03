package com.example.mjg.spring.filtering;

import com.example.mjg.data.DataFilterSet;
import com.example.mjg.data.DataPage;
import com.example.mjg.data.MigratableEntity;
import com.example.mjg.data.SimpleDataPage;
import com.example.mjg.spring.exceptions.InvalidRepositoryMethodException;
import com.example.mjg.spring.repositories.MigratableSpringRepository;
import com.example.mjg.spring.stores.SpringRepositoryStore;
import com.example.mjg.utils.ReflectionUtils;
import com.example.mjg.utils.functional.interfaces.*;

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

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D
    >
    SpringRepositoryFilterSet<T, ID> of(Function6<REPO, A, B, C, D, Pageable, Page<T>> lambda, A a, B b, C c, D d) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E
    >
    SpringRepositoryFilterSet<T, ID> of(Function7<REPO, A, B, C, D, E, Pageable, Page<T>> lambda, A a, B b, C c, D d, E e) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F
    >
    SpringRepositoryFilterSet<T, ID> of(Function8<REPO, A, B, C, D, E, F, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G
    >
    SpringRepositoryFilterSet<T, ID> of(Function9<REPO, A, B, C, D, E, F, G, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G, H
    >
    SpringRepositoryFilterSet<T, ID> of(Function10<REPO, A, B, C, D, E, F, G, H, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g, H h) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g, h)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G, H, I
    >
    SpringRepositoryFilterSet<T, ID> of(Function11<REPO, A, B, C, D, E, F, G, H, I, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g, H h, I i) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g, h, i)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G, H, I, J
    >
    SpringRepositoryFilterSet<T, ID> of(Function12<REPO, A, B, C, D, E, F, G, H, I, J, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g, H h, I i, J j) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g, h, i, j)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G, H, I, J, K
    >
    SpringRepositoryFilterSet<T, ID> of(Function13<REPO, A, B, C, D, E, F, G, H, I, J, K, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g, h, i, j, k)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G, H, I, J, K, L
    >
    SpringRepositoryFilterSet<T, ID> of(Function14<REPO, A, B, C, D, E, F, G, H, I, J, K, L, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g, h, i, j, k, l)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G, H, I, J, K, L, M
    >
    SpringRepositoryFilterSet<T, ID> of(Function15<REPO, A, B, C, D, E, F, G, H, I, J, K, L, M, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g, h, i, j, k, l, m)
        );
    }

    public static <
        T extends MigratableEntity,
        ID extends Serializable,
        REPO extends MigratableSpringRepository<T, ID>,
        A, B, C, D, E, F, G, H, I, J, K, L, M, N
    >
    SpringRepositoryFilterSet<T, ID> of(Function16<REPO, A, B, C, D, E, F, G, H, I, J, K, L, M, N, Pageable, Page<T>> lambda,
                                        A a, B b, C c, D d, E e, F f, G g, H h, I i, J j, K k, L l, M m, N n) {
        return new SpringRepositoryFilterSet<>(
            ReflectionUtils.extractMemberMethodFromLambda(lambda),
            List.of(a, b, c, d, e, f, g, h, i, j, k, l, m, n)
        );
    }
}
