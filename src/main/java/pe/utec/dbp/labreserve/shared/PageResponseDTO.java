package pe.utec.dbp.labreserve.shared;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envoltura de paginacion con la forma que pide el enunciado:
 * content / page / size / totalElements.
 */
public record PageResponseDTO<T>(List<T> content, int page, int size, long totalElements) {

    public static <E, T> PageResponseDTO<T> map(Page<E> page, Function<E, T> mapper) {
        return new PageResponseDTO<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}
