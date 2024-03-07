package com.mafia.mafiabackend.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteDimensionRequest;
import com.google.api.services.sheets.v4.model.DeleteRangeRequest;
import com.google.api.services.sheets.v4.model.DimensionRange;
import com.google.api.services.sheets.v4.model.GridRange;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.mafia.mafiabackend.dto.SimpleStatisticDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GoogleSheetsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleSheetsService.class);
    public static final InsertDimensionRequest INSERT_DIMENSION_REQUEST = new InsertDimensionRequest()
            .setRange(new DimensionRange().setDimension("ROWS")
                    .setStartIndex(1)
                    .setEndIndex(11));
    public static final DeleteDimensionRequest DELETE_DIMENSION_REQUEST = new DeleteDimensionRequest()
            .setRange(new DimensionRange().setDimension("ROWS")
                    .setStartIndex(2)
                    .setEndIndex(10000));
    private final String spreadsheetId = "1AoHIfdopqB3TcALJ5p5NEOOo34MnFYR-70aMWlfEAPs";
    private static final String APPLICATION_NAME = "Google Sheets API for Vyasma Mafia";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES =
            Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final Path CREDENTIALS_FILE_PATH = Path.of("/tokens/keen-shape-408018-b69a07dd9cf0.json");
    final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();

    private final Sheets service;

    private final boolean enableGoogleApi;

    public GoogleSheetsService(
            @Value("${api.google.enable}") boolean enableGoogleApi
    ) throws GeneralSecurityException, IOException {
        this.enableGoogleApi = enableGoogleApi;
        if (checkDisabled()) {
            service = null;
            return;
        }
        service = new Sheets.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    private boolean checkDisabled() {
        return !enableGoogleApi;
    }

    /**
     * Creates an authorized Credential object.
     *
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static HttpCredentialsAdapter getCredentials()
            throws IOException {
        GoogleCredentials googleCredentials;
        try (InputStream inputSteam = Files.newInputStream(CREDENTIALS_FILE_PATH)) {
            googleCredentials = GoogleCredentials.fromStream(Objects.requireNonNull(inputSteam)).createScoped(SCOPES);
        }
        return new HttpCredentialsAdapter(googleCredentials);
    }

    public void deleteAllPreviousResults() {
        if (checkDisabled()) {
            return;
        }
        try {
            service.spreadsheets()
                    .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                            .setRequests(List.of(
                                    new Request().setDeleteDimension(DELETE_DIMENSION_REQUEST),
                                    new Request().setInsertDimension(INSERT_DIMENSION_REQUEST),
                                    new Request().setDeleteDimension(DELETE_DIMENSION_REQUEST)
                            )))
                    .execute();
            service.spreadsheets()
                    .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                            .setRequests(List.of(
                                            new Request().setDeleteRange(new DeleteRangeRequest()
                                                    .setRange(new GridRange()
                                                            .setStartRowIndex(1)
                                                            .setEndRowIndex(1)
                                                            .setStartColumnIndex(0)
                                                            .setEndColumnIndex(30)))
                                    )
                            )
                    )
                    .execute();
        } catch (IOException e) {
            logger.error("Error on writing in google table", e);
        }
    }

    public void sendResultStatToGoogleSheet(List<SimpleStatisticDto> simpleStatistic) {
        if (checkDisabled()) {
            return;
        }
        var statisticTable = simpleStatistic.stream()
                .map(it -> List.<Object>of(
                        prettyDate(it.getGameDate()),
                        it.getGameId(),
                        it.getPlayerName(),
                        it.getSitNumber(),
                        it.getIsRedWin(),
                        it.getIsRed(),
                        it.getBestTurn(),
                        it.getRole().name(),
                        it.getSeason(),
                        it.isFirstKilled()
                )).toList();
        try {
            service.spreadsheets()
                    .batchUpdate(spreadsheetId, new BatchUpdateSpreadsheetRequest()
                            .setRequests(List.of(new Request().setInsertDimension(INSERT_DIMENSION_REQUEST))))
                    .execute();
            //    gameDate,gameId,playerName,redWin,isRed,bestTurn,role,season,firstKilled
            service.spreadsheets()
                    .values()
                    .update(spreadsheetId, "A2:J11", new ValueRange().setValues(statisticTable))
                    .setValueInputOption("USER_ENTERED")
                    .execute();
        } catch (IOException e) {
            logger.error("Error on writing in google table", e);
        }

    }

    private String prettyDate(Instant instant) {
        ZoneId zone = ZoneId.of("UTC+3");
        Locale ru = Locale.forLanguageTag("ru");
        return instant.atZone(zone).toLocalDateTime()
                .format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL, FormatStyle.SHORT).withLocale(ru));
    }
}
