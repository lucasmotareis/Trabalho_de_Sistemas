"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Box from "@mui/material/Box";
import CircularProgress from "@mui/material/CircularProgress";
import { authService } from "@/services/auth.service";
import { clearStoredSession, setStoredSession } from "@/lib/auth";

export default function AuthGuard({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  const router = useRouter();
  const [isChecking, setIsChecking] = useState(true);
  const [isAllowed, setIsAllowed] = useState(false);

  useEffect(() => {
    let active = true;

    const checkSession = async () => {
      setIsChecking(true);
      try {
        const me = await authService.me();
        if (!active) {
          return;
        }
        setStoredSession({
          user: {
            id: String(me.id),
            name: me.nome,
            email: me.email,
          },
        });
        setIsAllowed(true);
      } catch {
        if (!active) {
          return;
        }
        clearStoredSession();
        setIsAllowed(false);
        router.replace("/login");
      } finally {
        if (active) {
          setIsChecking(false);
        }
      }
    };

    checkSession();

    return () => {
      active = false;
    };
  }, [router]);

  if (isChecking || !isAllowed) {
    return (
      <Box
        sx={{
          minHeight: "100dvh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  return children;
}
