"use client";

import { useState } from "react";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import Button from "@mui/material/Button";
import Card from "@mui/material/Card";
import Divider from "@mui/material/Divider";
import Stack from "@mui/material/Stack";
import TextField from "@mui/material/TextField";
import Typography from "@mui/material/Typography";
import VerificationResultCard from "./VerificationResultCard";
import { verifyService } from "@/services/verify.service";
import type { VerifyManualRequest, VerifyByPublicIdResponse } from "@/types/verify";

const initialPayload: VerifyManualRequest = {
  publicId: "",
  text: "",
  signatureBase64: "",
};

export default function VerifySignatureCard() {
  const [publicId, setPublicId] = useState("");
  const [payload, setPayload] = useState<VerifyManualRequest>(initialPayload);
  const [result, setResult] = useState<VerifyByPublicIdResponse | null>(null);
  const [isVerifyingId, setIsVerifyingId] = useState(false);
  const [isVerifyingPayload, setIsVerifyingPayload] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleVerifyById = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (!publicId.trim()) {
      setError("Informe o ID publico da assinatura.");
      return;
    }

    setIsVerifyingId(true);
    try {
      const response = await verifyService.verifyByPublicId(publicId.trim());
      setResult(response);
    } catch (verifyError) {
      const message =
        verifyError instanceof Error
          ? verifyError.message
          : "Nao foi possivel verificar o ID informado.";
      setError(message);
    } finally {
      setIsVerifyingId(false);
    }
  };

  const handleVerifyByPayload = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);

    if (!payload.publicId.trim() || !payload.text.trim() || !payload.signatureBase64.trim()) {
      setError("Informe ID publico, texto e assinatura para verificar.");
      return;
    }

    setIsVerifyingPayload(true);
    try {
      const response = await verifyService.verifyManual({
        publicId: payload.publicId.trim(),
        text: payload.text.trim(),
        signatureBase64: payload.signatureBase64.trim(),
      });
      setResult(response);
    } catch (verifyError) {
      const message =
        verifyError instanceof Error
          ? verifyError.message
          : "Nao foi possivel verificar texto e assinatura.";
      setError(message);
    } finally {
      setIsVerifyingPayload(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 900, mx: "auto", p: { xs: 2, sm: 4 } }}>
      <Stack spacing={3}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Verificacao publica de assinatura
        </Typography>

        {error ? <Alert severity="error">{error}</Alert> : null}

        <Card variant="outlined" sx={{ p: 3 }}>
          <Stack spacing={2} component="form" onSubmit={handleVerifyById}>
            <Typography variant="h6">Verificar por ID publico</Typography>
            <TextField
              label="ID publico da assinatura"
              value={publicId}
              onChange={(event) => setPublicId(event.target.value)}
              fullWidth
            />
            <Button type="submit" variant="contained" disabled={isVerifyingId}>
              {isVerifyingId ? "Verificando..." : "Verificar ID"}
            </Button>
          </Stack>
        </Card>

        <Divider>ou</Divider>

        <Card variant="outlined" sx={{ p: 3 }}>
          <Stack spacing={2} component="form" onSubmit={handleVerifyByPayload}>
            <Typography variant="h6">Verificar por texto + assinatura</Typography>
            <TextField
              label="ID publico"
              value={payload.publicId}
              onChange={(event) =>
                setPayload((prev) => ({ ...prev, publicId: event.target.value }))
              }
              fullWidth
            />
            <TextField
              label="Texto"
              multiline
              minRows={5}
              value={payload.text}
              onChange={(event) =>
                setPayload((prev) => ({ ...prev, text: event.target.value }))
              }
              fullWidth
            />
            <TextField
              label="Assinatura (Base64)"
              multiline
              minRows={4}
              value={payload.signatureBase64}
              onChange={(event) =>
                setPayload((prev) => ({ ...prev, signatureBase64: event.target.value }))
              }
              fullWidth
            />
            <Button type="submit" variant="contained" disabled={isVerifyingPayload}>
              {isVerifyingPayload ? "Verificando..." : "Verificar assinatura"}
            </Button>
          </Stack>
        </Card>

        <VerificationResultCard result={result} />
      </Stack>
    </Box>
  );
}
