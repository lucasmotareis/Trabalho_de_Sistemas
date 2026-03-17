import VerifyByIdPage from "@/components/signature/VerifyByIdPage";

interface VerifyByIdRouteProps {
  params: Promise<{
    publicId: string;
  }>;
}

export default async function VerifyByIdRoute({ params }: VerifyByIdRouteProps) {
  const { publicId } = await params;
  return <VerifyByIdPage publicId={publicId} />;
}
