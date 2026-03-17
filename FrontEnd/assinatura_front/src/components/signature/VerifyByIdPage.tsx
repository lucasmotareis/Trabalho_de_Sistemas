"use client";

import { useEffect, useState } from "react";
import NextLink from "next/link";
import Alert from "@mui/material/Alert";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import Link from "@mui/material/Link";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import VerificationResultCard from "./VerificationResultCard";
import { verifyService } from "@/services/verify.service";
import type { VerifyByPublicIdResponse } from "@/types/verify";

interface VerifyByIdPageProps {
  publicId: string;
}

export default function VerifyByIdPage({ publicId }: VerifyByIdPageProps) {
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [result, setResult] = useState<VerifyByPublicIdResponse | null>(null);

  useEffect(() => {
    let active = true;

    const runVerification = async () => {
      setIsLoading(true);
      setError(null);

      try {
        const response = await verifyService.verifyByPublicId(publicId);
        if (active) {
          setResult(response);
        }
      } catch (verifyError) {
        if (active) {
          const message =
            verifyError instanceof Error
              ? verifyError.message
              : "Nao foi possivel verificar este ID.";
          setError(message);
        }
      } finally {
        if (active) {
          setIsLoading(false);
        }
      }
    };

    runVerification();

    return () => {
      active = false;
    };
  }, [publicId]);

  return (
    <Box sx={{ maxWidth: 900, mx: "auto", p: { xs: 2, sm: 4 } }}>
      <Stack spacing={3}>
        <Typography variant="h4" sx={{ fontWeight: 700 }}>
          Verificacao por ID publico
        </Typography>
        <Typography variant="body1" color="text.secondary">
          ID consultado: <strong>{publicId}</strong>
        </Typography>

        {isLoading ? (
          <Stack direction="row" spacing={2} alignItems="center">
            <CircularProgress size={24} />
            <Typography variant="body2">Consultando assinatura...</Typography>
          </Stack>
        ) : null}

        {error ? <Alert severity="error">{error}</Alert> : null}

        {!isLoading && !error ? <VerificationResultCard result={result} /> : null}

        <Link component={NextLink} href="/verify" underline="hover">
          Voltar para verificacao publica
        </Link>
      </Stack>
    </Box>
  );
}
