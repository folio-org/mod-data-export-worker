package org.folio.model;

import org.folio.dto.GreetingDto;
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.UUID;

public class RestApiGreetingItemReader extends AbstractItemCountingItemStreamItemReader<GreetingDto> {

    private static final int NUMBER_OF_GREETINGS_TO_RETRIEVE_PER_HTTP_REQUEST = 100;

    private static final boolean IS_SAVE_READER_STATE = false;

    private String getGreetingsMethodUrlTemplate;

    private RestTemplate restTemplate;

    private int currentOffset;

    private GreetingDto[] currentGreetingDtoChunk;

    private int currentGreetingDtoChunkOffset;

    public RestApiGreetingItemReader(String getGreetingsMethodUrlTemplate, RestTemplate restTemplate, int offset, int limit) {
        this.getGreetingsMethodUrlTemplate = getGreetingsMethodUrlTemplate;
        this.restTemplate = restTemplate;
        this.currentOffset = offset;

        this.setCurrentItemCount(0);
        this.setMaxItemCount(limit);
        this.setSaveState(IS_SAVE_READER_STATE);
        this.setExecutionContextName("RestApiGreetingItemReader_" + UUID.randomUUID());
    }

    @Override
    protected GreetingDto doRead() throws Exception {
        if (this.currentGreetingDtoChunk == null || this.currentGreetingDtoChunkOffset >= this.currentGreetingDtoChunk.length) {
            this.currentGreetingDtoChunk = getGreetings(this.restTemplate, this.getGreetingsMethodUrlTemplate, this.currentOffset);
            this.currentOffset += NUMBER_OF_GREETINGS_TO_RETRIEVE_PER_HTTP_REQUEST;
            this.currentGreetingDtoChunkOffset = 0;
        }

        if (this.currentGreetingDtoChunk == null || this.currentGreetingDtoChunk.length == 0) {
            return null;
        }

        GreetingDto greeting = this.currentGreetingDtoChunk[this.currentGreetingDtoChunkOffset];
        this.currentGreetingDtoChunkOffset++;

        return greeting;
    }

    @Override
    protected void doOpen() throws Exception {
    }

    @Override
    protected void doClose() throws Exception {
    }

    private GreetingDto[] getGreetings(RestTemplate restTemplate, String getGreetingsMethodUrlTemplate, int requestOffset) {
        // TODO validate input parameters here

        String getGreetingsMethodUrl = String.format(getGreetingsMethodUrlTemplate, requestOffset, NUMBER_OF_GREETINGS_TO_RETRIEVE_PER_HTTP_REQUEST);
        ResponseEntity<GreetingDto[]> response = restTemplate.getForEntity(getGreetingsMethodUrl, GreetingDto[].class);

        // TODO handle 500 and 400 errors here

        GreetingDto[] greetingsChunk = response.getBody();
        return greetingsChunk;
    }
}
