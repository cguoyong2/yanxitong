package com.yanxitong.invitation;

import com.yanxitong.invitation.dto.TemplatePresentation;
import com.yanxitong.template.entity.InvitationTemplate;
import org.springframework.stereotype.Service;

@Service
public class TemplatePresentationService {
    public TemplatePresentation resolve(InvitationTemplate template, String eventTypeCode) {
        String templateCode = template == null ? "" : template.templateCode;
        if (templateCode.contains("WEDDING") || "WEDDING".equals(eventTypeCode)) {
            return new TemplatePresentation("wedding-red-gold", "良辰喜宴", "诚邀您拨冗赴宴，共同见证这份喜悦。", "17:30 来宾签到\n18:00 仪式开始\n18:30 喜宴开席", "喜");
        }
        if (templateCode.contains("BIRTHDAY") || "BIRTHDAY".equals(eventTypeCode)) {
            return new TemplatePresentation("birthday-warm", "福寿喜宴", "诚邀亲友同聚一堂，共祝福寿安康。", "17:30 来宾签到\n18:00 寿宴仪式\n18:30 宴席开席", "寿");
        }
        if (templateCode.contains("BABY") || "BABY".equals(eventTypeCode)) {
            return new TemplatePresentation("baby-garden", "满月之喜", "诚邀您一同分享宝宝成长的温暖时刻。", "11:00 来宾签到\n11:30 满月仪式\n12:00 午宴开席", "满");
        }
        if (templateCode.contains("HOUSE") || "HOUSEWARMING".equals(eventTypeCode)) {
            return new TemplatePresentation("house-modern", "乔迁雅宴", "新居落成，诚邀您莅临相聚，共叙情谊。", "17:30 来宾签到\n18:00 乔迁仪式\n18:30 晚宴开席", "乔");
        }
        if (templateCode.contains("SCHOOL") || "SCHOOL".equals(eventTypeCode)) {
            return new TemplatePresentation("school-honor", "升学答谢", "感谢一路关怀与陪伴，诚邀您共赴升学答谢宴。", "17:30 来宾签到\n18:00 答谢致辞\n18:30 宴席开席", "学");
        }
        if (templateCode.contains("MEMORIAL") || "MEMORIAL".equals(eventTypeCode)) {
            return new TemplatePresentation("memorial-simple", "追思相聚", "谨以素心相邀，共同缅怀与追忆。", "09:30 来宾签到\n10:00 追思仪式\n11:00 礼成送别", "忆");
        }
        return new TemplatePresentation("general-warm", "诚挚邀请", "诚邀您拨冗赴宴，共同见证这份重要时刻。", "17:30 来宾签到\n18:00 宴席开始\n18:30 宾主同欢", "宴");
    }
}
