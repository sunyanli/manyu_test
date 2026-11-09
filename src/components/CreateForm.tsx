"use client";

import { useState } from "react";
import type { CSSProperties } from "react";
import { Button, DatePicker, Dialog, Form, Input, TextArea, Toast } from "antd-mobile";
import { createTodo } from "@/lib/api";
import { DESC_MAX, NAME_MAX } from "@/lib/constants";
import { useI18n } from "@/i18n";
import { SiteHeader } from "./SiteHeader";
import { CalendarIcon, EditIcon, NoteIcon } from "./icons";

interface FormValues {
  name?: string;
  description?: string;
  dueAt?: Date | null;
}

function dueLabel(d: Date | null): string {
  if (!d) return "";
  const p = (n: number) => String(n).padStart(2, "0");
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
}

/** 新建待办表单（名称必填 + 描述选填 + 截止时间选填）。 */
export function CreateForm() {
  const { t } = useI18n();
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const watched = Form.useWatch(["name", "description", "dueAt"], form) as FormValues | undefined;

  const goBack = () => {
    const dirty = Boolean(watched?.name || watched?.description || watched?.dueAt);
    if (!dirty) {
      if (window.history.length > 1) window.history.back();
      else window.location.assign("/");
      return;
    }
    Dialog.confirm({
      title: t("discardTitle"),
      content: t("discardDesc"),
      confirmText: t("discardOk"),
      cancelText: t("discardCancel"),
      onConfirm: () => {
        if (window.history.length > 1) window.history.back();
        else window.location.assign("/");
      },
    });
  };

  const onSubmit = async (values: FormValues) => {
    const name = (values.name ?? "").trim();
    if (!name) {
      Toast.show({ icon: "fail", content: t("nameRequired") });
      return;
    }
    if (name.length > NAME_MAX) {
      Toast.show({ icon: "fail", content: t("nameTooLong") });
      return;
    }
    const description = (values.description ?? "").trim();
    if (description.length > DESC_MAX) {
      Toast.show({ icon: "fail", content: t("descTooLong") });
      return;
    }

    setSubmitting(true);
    try {
      await createTodo({ name, description, dueAt: values.dueAt ? values.dueAt.toISOString() : null });
      Toast.show({ icon: "success", content: t("created") });
      // 成功后回到列表并置顶新项：用 replace 避免历史栈残留新建页。
      window.setTimeout(() => window.location.replace("/"), 400);
    } catch {
      setSubmitting(false);
      Toast.show({ icon: "fail", content: t("createFailed") });
    }
  };

  return (
    <div className="page">
      <SiteHeader title={t("createTitle")} showBack onBack={goBack} />
      <main className="page__main form-main">
        <Form form={form} onFinish={onSubmit} layout="vertical" requiredMarkStyle="none">
          <Form.Item
            name="name"
            label={
              <span className="form-label">
                <EditIcon size={18} />
                <span>{t("nameLabel")}</span>
                <em className="form-required">*</em>
              </span>
            }
            rules={[
              { required: true, message: t("nameRequired") },
              { max: NAME_MAX, message: t("nameTooLong") },
            ]}
          >
            <Input placeholder={t("namePlaceholder")} maxLength={NAME_MAX} clearable className="form-input" />
          </Form.Item>

          <Form.Item
            name="description"
            label={
              <span className="form-label">
                <NoteIcon size={18} />
                <span>{t("descLabel")}</span>
              </span>
            }
            rules={[{ max: DESC_MAX, message: t("descTooLong") }]}
          >
            <TextArea
              placeholder={t("descPlaceholder")}
              maxLength={DESC_MAX}
              rows={3}
              showCount
              className="form-textarea"
              style={{ "--font-size": "16px" } as CSSProperties}
            />
          </Form.Item>

          <Form.Item
            name="dueAt"
            trigger="onConfirm"
            label={
              <span className="form-label">
                <CalendarIcon size={18} />
                <span>{t("dueLabel")}</span>
              </span>
            }
          >
            <DatePicker precision="minute">
              {(value) =>
                value ? (
                  <div className="due-value">
                    <span className="mono">{dueLabel(value)}</span>
                    <button
                      type="button"
                      className="due-clear"
                      onClick={(e) => {
                        e.stopPropagation();
                        form.setFieldValue("dueAt", null);
                      }}
                    >
                      {t("dueClear")}
                    </button>
                  </div>
                ) : (
                  <div className="due-value due-value--unset">{t("dueUnset")} ▸</div>
                )
              }
            </DatePicker>
          </Form.Item>
        </Form>
      </main>
      <div className="form-footer">
        <Button block color="primary" loading={submitting} disabled={submitting} onClick={() => form.submit()}>
          {submitting ? t("saving") : t("save")}
        </Button>
      </div>
    </div>
  );
}
