package com.SouthMillion.globalserver_service.service.impl;

import org.SouthMillion.dto.globalserver.MailAckInfo;
import org.SouthMillion.dto.globalserver.MailDTO;

import java.util.List;

public interface MailService {
    List<MailDTO> getMailList(Long userId);

    MailDTO getMailDetail(Long userId, Integer mailIndex);

    List<MailAckInfo> deleteMails(Long userId, List<Integer> mailIndexes);

    List<MailAckInfo> fetchMails(Long userId, List<Integer> mailIndexes);

    MailAckInfo mailOperation(Long userId, Integer type, Integer p1, Integer p2);
}