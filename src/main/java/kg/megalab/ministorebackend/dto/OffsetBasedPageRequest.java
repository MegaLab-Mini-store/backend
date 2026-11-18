package kg.megalab.ministorebackend.dto;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record OffsetBasedPageRequest(long offset, int limit, Sort sort) implements Pageable {

    public OffsetBasedPageRequest {
        if (offset < 0) {
            throw new IllegalArgumentException("Offset must not be less than zero");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit must not be less than one");
        }
        if (sort == null) {
            sort = Sort.unsorted();
        }
    }

    public OffsetBasedPageRequest(long offset, int limit) {
        this(offset, limit, Sort.unsorted());
    }

    @Override
    public int getPageNumber() {
        return (int) (offset / limit);
    }

    @Override
    public int getPageSize() {
        return limit;
    }

    @Override
    public long getOffset() {
        return offset;
    }

    @Override
    @NullMarked
    public Sort getSort() {
        return sort;
    }

    @Override
    @NullMarked
    public Pageable next() {
        return new OffsetBasedPageRequest(getOffset() + getPageSize(), getPageSize(), getSort());
    }

    @Override
    @NullMarked
    public Pageable previousOrFirst() {
        return hasPrevious()
                ? new OffsetBasedPageRequest(getOffset() - getPageSize(), getPageSize(), getSort())
                : first();
    }

    @Override
    @NullMarked
    public Pageable first() {
        return new OffsetBasedPageRequest(0, getPageSize(), getSort());
    }

    @Override
    @NullMarked
    public Pageable withPage(int pageNumber) {
        return new OffsetBasedPageRequest((long) pageNumber * getPageSize(), getPageSize(), getSort());
    }

    @Override
    public boolean hasPrevious() {
        return offset >= limit;
    }
}
