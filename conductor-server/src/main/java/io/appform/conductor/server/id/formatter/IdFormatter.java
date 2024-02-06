package io.appform.conductor.server.id.formatter;

import org.joda.time.DateTime;

public interface IdFormatter {

    String format(final DateTime dateTime,
                  final int nodeId,
                  final int randomNonce);


}
